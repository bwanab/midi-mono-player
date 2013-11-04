(ns midi-mono-player.monitor
  (:use [seesaw core]
        [midi-mono-player wx7 test-wx7])
  (:require [overtone.libs.event :as e]))

(native!)



(def event-key [::midi-mono-player.wx7/wx7-event])

(def mono-player-events (atom {}))

(defn make-continuous-vals-panel
  [events]
  (let [items (for [[n p] events]
                (-> (grid-panel :rows 2 :columns 1)
                    (add! (label (str n ": " (:name p))))
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
                    (add! (label (str n ": " (:name p))))
                    (add! (let [t (text (str (:default p)))]
                            (swap! mono-player-events assoc (:symbol p) {:type :discreet :widget t})
                            t))))]
    (grid-panel :rows 1 :columns (count items) :border "" :items items)))

(defn make-main-panel
  [discreet-cc-events pc-switches continuous-cc-events]
  (border-panel :north (-> (grid-panel :rows 3 :columns 1)
                           (add! (make-switches-panel discreet-cc-events))
                           (add! (make-switches-panel pc-switches)))
                :center (make-continuous-vals-panel continuous-cc-events)))

(defn make-frame
  [cc-events pc-switches]
  (frame
    :title "WX7 Monitor"
    :size  [600 :by 600]
    :on-close :exit
    :content (border-panel
               :center (make-main-panel (filter #(= :discreet (:type (val %))) cc-events)
                                        pc-switches
                                        (filter #(not= :discreet (:type (val %))) cc-events)))))


(defn monitor
  [play-fn midi-map]
  (let [cc-events    (get-midi-defs play-fn (:control-change midi-map))
        pc-switches  (get-midi-defs play-fn (:program-change midi-map))]
    (invoke-later
     (-> (make-frame cc-events pc-switches) pack! show!)))
  (e/on-event [:wx7-event]
              (fn [val]
                (when-let [e (get @mono-player-events (:type val))]
                  (case (:type e)
                    :continuous (config! (:widget e) :value  (* 100 (:val val)))
                    :discreet (text! (:widget e) (str (:val val))))))
              (concat event-key [:wx7-event])))

(monitor wx7mooger fcb-map)
