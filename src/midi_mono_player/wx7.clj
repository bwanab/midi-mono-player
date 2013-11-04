(ns midi-mono-player.wx7
    (:use [overtone.studio.midi]
          [overtone.sc.node]
          [overtone.sc.dyn-vars])
    (:require [overtone.libs.event :as e]))

"
    (definst ding
      [note 60 velocity 100 gate 1]
      (let [freq (midicps note)
            amp  (/ velocity 127.0)
            snd  (sin-osc freq)
            env  (env-gen (adsr 0.001 0.1 0.6 0.3) gate :action FREE)]
        (* amp env snd)))
"

"
central-bend-point is the value of :velocity in a pitch-bench event that is most common with normal emboucher.
This is an observed value.
"
(def central-bend-point 78.0)
"
The distribution of values of :velocity was a rough bell curve around the central point with min values of 68
and max values of 90. I want a bend of about a demi-tone in each direction. This computes to a -0.1 factor for each
value below 78 and +0.1 for each value above.
"
(def bend-factor 0.1)

(defn compute-note
  [raw-note bend]
  (+ raw-note (* bend-factor (- bend central-bend-point))))
(def cc-breath-control 2)

(defn- get-param [params param]
  (first (filter (fn [e] (= (:name e) param)) params)))

(defn- munge-param [p type]
  {:symbol (keyword (:name p))
   :offset (:min p)
   :range (- (:max p) (:min p))
   :type type})

(defn get-midi-defs
  "for each of the switchs get the control meta-data from the play-fn and compute the
offset, range, symbol and type to use when the event occurs "
  [play-fn switches]
  (apply merge
         (let [params (:params play-fn)]
           (for [[num param type] switches]
             (when-let [p (get-param params param)]
               {num (merge p (munge-param p type))})))))

(defn fire-event [s val]
  (e/event  [:wx7-event s] {:val val}))

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

(defn yamaha-wx7
  ([play-fn] (yamaha-wx7  play-fn '{}))
  ([play-fn midi-map] (yamaha-wx7  play-fn midi-map ::yamaha-wx7))
  ([play-fn midi-map player-key] (yamaha-wx7 play-fn midi-map [:midi] player-key))
  ([play-fn midi-map device-key player-key]
     (let [synth         (play-fn :note 48 :amp 0 :velocity 0)
           last-note*    (atom   {:note 48})
           on-event-key  (concat device-key [:note-on])
           pb-event-key  (concat device-key [:pitch-bend])
           cc-event-key  (concat device-key [:control-change])
           pc-event-key  (concat device-key [:program-change])
           on-key        (concat [::yamaha-wx7] on-event-key)
           pb-key        (concat [::yamaha-wx7] pb-event-key)
           cc-key        (concat [::yamaha-wx7] cc-event-key)
           pc-key        (concat [::yamaha-wx7] pc-event-key)
           cc-events    (get-midi-defs play-fn (:control-change midi-map))
           pc-switches  (get-midi-defs play-fn (:program-change midi-map))]

       (e/on-event on-event-key (fn [{note :note velocity :velocity}]
                                  (let [amp (float (/ velocity 127))]
                                    (with-inactive-node-modification-error :silent
                                      (node-control synth [:note note :amp amp :velocity velocity]))
                                    (swap! last-note* assoc
                                           :note* note)))
                   on-key)

       "off event isn't needed since the wx7 is at rest unless I'm blowing."

       (e/on-event pb-event-key (fn [{dummy :note bend :velocity}]
                                  (if-let [raw-note (:note* @last-note*)]
                                    (let [note (compute-note raw-note bend)]
                                      (with-inactive-node-modification-error :silent
                                        (node-control synth [:note note])))))
                   pb-key)

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
                      {:type ::yamaha-wx7})]
         (swap! poly-players* assoc player-key player)))))
