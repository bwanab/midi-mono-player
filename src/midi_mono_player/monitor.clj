(ns midi-mono-player.monitor
  (:use [seesaw core style font]
        [midi-mono-player player test-wx7])
  (:require [overtone.libs.event :as e]))

(native!)



(def event-key [::midi-mono-player.player/mono-midi-player-event])

(def mono-player-events (atom {}))

(defn make-continuous-vals-panel
  [events]
  (let [items (for [[n p] events]
                (-> (grid-panel :rows 2 :columns 1)
                    (add! (label :text (str n ": " (:name p)) :class :event-title))
                    (add! (let [t (progress-bar :orientation :vertical
                                                :min (* 100 (:min p))
                                                :max (* 100 (:max p))
                                                :value (* 100 (:default p)))]
                            (swap! mono-player-events assoc (:symbol p) {:type :continuous :widget t})
                            t))))]
    (grid-panel :rows 1 :columns (count items) :border "" :items items)))


(defn make-switches-panel
  [events]
  (let [items (for [[n p] events]
                (-> (grid-panel  :rows 2 :columns 1)
                    (add! (label :text (str n ": " (:name p)) :class :event-title))
                    (add! (let [t (text (str (:default p)))]
                            (swap! mono-player-events assoc (:symbol p) {:type :discreet :widget t})
                            t))))]
    (grid-panel :rows 1 :columns (count items) :border "" :items items)))

(defn make-main-panel
  ([cc-events] (make-main-panel cc-events nil))
  ([cc-events pc-switches]
     (let [discreet-cc-events (filter #(= :discreet (:type (val %))) cc-events)
           continuous-cc-events (filter #(not= :discreet (:type (val %))) cc-events)]
       (border-panel :north (-> (grid-panel :rows 3 :columns 1)
                                (add! (make-switches-panel discreet-cc-events))
                                (add! (if pc-switches
                                        (make-switches-panel pc-switches)
                                        (label ""))))
                     :center (make-continuous-vals-panel continuous-cc-events)))))

(defn make-exit
  [kill-fn]
  (-> (flow-panel)
      (add! (button :text "Exit"
                    :listen [:action (fn [e] (kill-fn))]))))
(defn make-frame
  [cc-events pc-switches name kill-fn]
  (frame
    :title name
    :size  [600 :by 600]
    :on-close :exit
    :content (border-panel
              :center (make-main-panel cc-events pc-switches)
              :south (make-exit kill-fn))))

(defn update-monitor
  [f play-fn midi-map name kill-fn]
  (let [cc-events    (get-midi-defs play-fn (:control-change midi-map))]
    (-> (config! f
                 :title name
                 :content (border-panel :center (make-main-panel cc-events)
                                        :south (make-exit kill-fn)))
        pack! show!)))

(defn kill-monitor
  [f]
  (dispose! f))

(defn monitor
  [play-fn midi-map name kill-fn]
  (let [cc-events    (get-midi-defs play-fn (:control-change midi-map))
        pc-switches  (get-midi-defs play-fn (:program-change midi-map))
        f (make-frame cc-events pc-switches name kill-fn)  ]
    (e/on-event [:mono-midi-player-event]
              (fn [val]
                (when-let [e (get @mono-player-events (:type val))]
                  (case (:type e)
                    :continuous (config! (:widget e) :value  (* 100 (:val val)))
                    :discreet (text! (:widget e) (str (:val val))))))
              (concat event-key [:mono-midi-player-event]))
    (apply-stylesheet f {[:.event-title] {:font (font :style :bold)}})
    (invoke-later
     (-> f pack! show!))
    f))

;(monitor wx7mooger wx7mooger-midi-map "wx7mooger")
