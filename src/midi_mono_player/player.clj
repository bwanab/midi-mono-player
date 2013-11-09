(ns midi-mono-player.player
    (:use [overtone.studio.midi]
          [overtone.sc.node]
          [overtone.sc.dyn-vars])
    (:require [overtone.libs.event :as e]))




(defn- get-param [params param]
  (first (filter (fn [e] (= (:name e) param)) params)))

(defn- munge-param [p type]
  {:symbol (keyword (:name p))
   :offset (:min p)
   :range (- (:max p) (:min p))
   :type type})

(defn get-midi-defs
  "for each of the switches get the control meta-data from the play-fn and compute the
offset, range, symbol and type to use when the event occurs "
  [play-fn switches]
  (if switches
    (apply merge
           (let [params (:params play-fn)]
             (for [[num param type] switches]
               (when-let [p (get-param params param)]
                 {num (merge p (munge-param p type))}))))))

(defn fire-event [s val]
  (e/event  [:mono-midi-player-event] {:type s :val val}))

(defn control-vals [p amp]
  (let [s (:symbol p)
        val (+ (:offset p)
               (* amp (:range p)))]
    (fire-event s val)
    [s val]))

(defn inc-max [x min max step]
  (let [r (+ x step)]
    (if (> r max) min r)))

(defn inc-switch [p]
  (swap! (:value p) inc-max (:min p) (:max p) (:step p)))

(defn discreet-change [p]
  (let [s (keyword (:name p))
        val (inc-switch p)]
    (fire-event s val)
    [s val]))

(defn play
  ([play-fn] (play  play-fn {:device-key [:midi] :player-key ::generic} {}))
  ([play-fn profile] (play play-fn profile {}))
  ([play-fn profile midi-map]
     (let [synth         (play-fn :note 48 :amp 0 :velocity 0)
           last-note*    (atom   {:note 48})
           device-key    (:device-key profile)
           player-key    (:player-key profile)
           central-bend-point (:central-bend-point profile 0.0)
           bend-factor        (:bend-factor profile 1.0)
           on-event-key  (concat device-key [:note-on])
           pb-event-key  (concat device-key [:pitch-bend])
           cc-event-key  (concat device-key [:control-change])
           pc-event-key  (concat device-key [:program-change])
           on-key        (concat [player-key] on-event-key)
           pb-key        (concat [player-key] pb-event-key)
           cc-key        (concat [player-key] cc-event-key)
           pc-key        (concat [player-key] pc-event-key)
           cc-events    (get-midi-defs play-fn (:control-change midi-map))
           pc-switches  (get-midi-defs play-fn (:program-change midi-map))]


       "note-on events send the new note to the synth and save that as the last-note played"
       (e/on-event on-event-key (fn [{note :note velocity :velocity}]
                                  (let [amp (float (/ velocity 127))]
                                    (with-inactive-node-modification-error :silent
                                      (node-control synth [:note note :amp amp :velocity velocity]))
                                    (swap! last-note* assoc
                                           :note* note)))
                   on-key)

       "off event isn't needed since the wx7 is at rest unless I'm blowing."

       "pitch-bend events mutate the the note that the synth is playing. bend-factor
and central-bend-point are specified in a separate profile for the specific device.

For example, given a last note played of middle c note is 48. This number is modified by pitch
bend to produce the resultant note where,

new-note = last-note + bend-factor * (bend - central-bend-point)

last-note is note affected until a new note-on event occurs.
"
       (e/on-event pb-event-key (fn [{dummy :note bend :velocity}]
                                  (if-let [raw-note (:note* @last-note*)]
                                    (let [note (+ raw-note (* bend-factor (- bend central-bend-point)))]
                                      (with-inactive-node-modification-error :silent
                                        (node-control synth [:note note])))))
                   pb-key)

       "control change events are interpreted based on the midi-map (see test-wx7 for examples).
All assigned control change events fire events of their own that are used by monitor to
display the current state"
       (e/on-event cc-event-key (fn [{cc :note velocity :velocity}]
                                  (when-let [p (get cc-events cc)]
                                    (try
                                      (let [amp (float (/ velocity 127))]
                                        (with-inactive-node-modification-error :silent
                                          (node-control synth (case (:type p)
                                                                :continuous (control-vals p amp)
                                                                :discreet (discreet-change p)))))
                                      (catch Exception e (println "unexpected cc: " cc))))
                                  )
                   cc-key)

       "program change events can be used if necessary, but it would be best not to use them to control
the player. They should be reserved for specifying what synth is currently running at a higher level (see
program.clj)"
       (e/on-event pc-event-key (fn [{program :note dummy :velocity}]
                                  (when-let [p (get pc-switches program)]
                                    (with-inactive-node-modification-error :silent
                                      (node-control synth (discreet-change p)))))
                   pc-key)

       ;; Todo listen for '/n_end' event for nodes that free themselves
       ;; before receiving a note-off message.
       (let [player (with-meta {:note* (:note* @last-note*)
                                :synth synth
                                :on-key on-key
                                :device-key device-key
                                :player-key player-key
                                :playing? (atom true)}
                      {:type player-key})]
         player))))
