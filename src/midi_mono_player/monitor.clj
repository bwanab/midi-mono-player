(ns midi-mono-player.monitor
  (:use [seesaw core style font]
        [midi-mono-player player test-wx7]
        [clojure.core.match :only (match)])
  (:require [overtone.libs.event :as e]))

(native!)


(defn oom [x] (last (filter (fn [n] (< (Math/abs (- x (Math/pow 10 n))) x))
                            (range -5 1))))

(defn get-format [x] (str "%." (Math/abs (oom x)) "f"))

(def event-key [:player/mono-midi-player-event])

(def mono-player-events (atom {}))

(defn make-continuous-vals-panel
  [events]
  (let [items (for [[n p] events]
                (border-panel :hgap 10
                              :north (label :h-text-position :center :text
                                            (str n ": " (:name p))
                                            :class :event-title)
                 :center (let [t (progress-bar :orientation :vertical
                                      :min (* 100 (:min p))
                                      :max (* 100 (:max p))
                                      :value (* 100 (:default p)))]
                           (swap! mono-player-events assoc (:symbol p)
                                  {:type :continuous :widget t})
                           t)))]
    (grid-panel :hgap 10 :rows 1 :columns (count items) :items items)))


(defn make-radio-event-widget
  [p f]
  (let [group (button-group)
        form-d (format f (float (:default p)))
        panel (vertical-panel
               :items (let [r (range (:min p) (+ (:max p) (:step p)) (:step p))]
                        (for [n r]
                          (let [form-n (format f (float n))]
                            (radio :text form-n :group group
                                   :selected? (= form-n form-d))))))]
    [panel group]))

(defn make-discreet-event-widget
  [p f]
  (match [(:min p) (:max p) (:step p)]
         [0 1 1] [:checkbox (checkbox :selected? (= (:default p) 1)) nil]
         :else (concat [:radio] (make-radio-event-widget p f))))

(defn make-switches-panel
  [events]
  (let [items (for [[n p] events]
                (border-panel :hgap 10
                              :north (label :h-text-position :center :text
                                            (str n ": " (:name p))
                                            :class :event-title)
                 :center (let [format (get-format (:step p))
                               [typ t g] (make-discreet-event-widget p format)]
                           (swap! mono-player-events assoc (:symbol p)
                                  {:type typ :widget t :group g :format format})
                           t)))]
    (grid-panel :hgap 10 :rows 1 :columns (count items) :border "" :items items)))

(defn make-main-panel
  ([cc-events] (make-main-panel cc-events nil))
  ([cc-events pc-switches]
   (let [discreet-cc-events
         (reverse (filter #(= :discreet (:type (val %))) cc-events))
         continuous-cc-events
         (reverse (filter #(= :continuous (:type (val %))) cc-events))]
       (border-panel :north (make-switches-panel discreet-cc-events)
                     :center (make-continuous-vals-panel continuous-cc-events)))))

(defn make-exit
  [kill-fn]
  (-> (flow-panel)
      (add! (button :text "Quit"
                    :listen [:action (fn [e] (kill-fn))]))))

(defn make-content-panel
  [cc-events pc-switches name kill-fn]
  (border-panel
   :north (label :text name :class :program-name
                 :h-text-position :center)
              :center (make-main-panel cc-events pc-switches)
              :south (make-exit kill-fn)))

(defn make-frame
  [cc-events pc-switches name kill-fn]
  (frame
    :title name
    :size  [600 :by 600]
    :on-close :exit
    :content (make-content-panel cc-events pc-switches name kill-fn)))

(defn do-stylesheet
  [f]
  (apply-stylesheet f {[:.event-title] {:font (font :style :bold)}
                       [:.program-name] {:font (font :style :bold :size 20)}}))

(defn update-monitor
  [f play-fn midi-map name kill-fn]
  (let [cc-events    (get-midi-defs play-fn (:control-change midi-map))]
    (config! f
             :title name
             :content (make-content-panel cc-events nil name kill-fn))
    (do-stylesheet f)
    (-> f pack! show!)))

(defn kill-monitor
  [f]
  (dispose! f))

(defn monitor
  [play-fn midi-map name kill-fn]
  (let [cc-events    (get-midi-defs play-fn (:control-change midi-map))
        pc-switches  (get-midi-defs play-fn (:program-change midi-map))
        f (make-frame cc-events pc-switches name kill-fn)  ]
    (e/on-event [:player/mono-midi-player-event]
                (fn [val]
                  (println "monitor event type = " (:type val))
                  (when-let [e (get @mono-player-events (:type val))]
                    (case (:type e)
                      :continuous (config! (:widget e) :value  (* 100 (:val val)))
                      :checkbox (config! (:widget e) :selected? (= val 1))
                      :radio (doseq [b (config (:group e) :buttons)]
                               (let [s (format (:format e) (float (:val val)))
                                     t (text b)]
                                 (config! b :selected? (= s t)))))))
              (concat event-key [:mono-midi-player-event]))
    (do-stylesheet f)
    (invoke-later
     (-> f pack! show!))
    f))

;(monitor wx7mooger wx7mooger-midi-map "wx7mooger")
