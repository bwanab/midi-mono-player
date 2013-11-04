(ns midi-mono-player.monitor
  (:use [seesaw core]
        [midi-mono-player wx7 test-wx7])
  (:require [overtone.libs.event :as e]))

(native!)


;; (defn make-main-panel
;;   [discreet-cc-events pc-switches]
;;   (-> (grid-panel :rows 3 :columns 1)
;;       (add! (let [fp (flow-panel)]
;;               (doseq [[n p] discreet-cc-events]
;;                 (add! fp (-> (grid-panel :rows 2 :columns 1)
;;                              (add! (label (str n ": " (:name p))))
;;                              (add! (text (str (:default p)))))))
;;               fp))))

(def event-key [::midi-mono-player.wx7/wx7-event])

(def mono-player-events (atom {}))

(defn make-switches-panel
  [events]
  (let [items (for [[n p] events]
                (-> (grid-panel :rows 2 :columns 1)
                    (add! (label (str n ": " (:name p))))
                    (add! (let [t (text (str (:default p)))
                                ek [:wx7-event (:symbol p)]]
                            (e/on-event ek
                                        (fn [val]
                                          (text! t (str (:val val))))
                                        (concat event-key ek))
                            t))))]
    (flow-panel :items items)))

(defn make-main-panel
  [discreet-cc-events pc-switches continuous-cc-events]
  (-> (grid-panel :rows 3 :columns 1)
      (add! (make-switches-panel discreet-cc-events))
      (add! (make-switches-panel pc-switches))
      (add! (make-switches-panel continuous-cc-events))))

(defn make-frame
  [cc-events pc-switches]
  (frame
    :title "WX7 Monitor"
    :size  [600 :by 600]
    :on-close :exit
    :menubar (menubar :items [(menu :text "View" :items [(menu-item :class :refresh)])])
    :content (border-panel
               :border 5
               :hgap 5
               :vgap 5
               :center (make-main-panel (filter #(= :discreet (:type (val %))) cc-events)
                                        pc-switches
                                        (filter #(not= :discreet (:type (val %))) cc-events))
               :south (label :id :status :text "Ready"))))


(defn monitor
  [play-fn midi-map]
  (let [cc-events    (get-midi-defs play-fn (:control-change midi-map))
        pc-switches  (get-midi-defs play-fn (:program-change midi-map))]
    (invoke-later
     (-> (make-frame cc-events pc-switches) pack! show!))))

(monitor wx7mooger fcb-map)
