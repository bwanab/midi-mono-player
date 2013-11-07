(ns midi-mono-player.monitor
  (:use [seesaw core style font]
        [midi-mono-player player test-wx7]
        [clojure.core.match :only (match)])
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


(defn make-radio-event-widget
  [p]
  (let [group (button-group)
        info-label (label :text "" :border 10)
        d (:default p)
        panel (vertical-panel
               :items (for [n (range (:min p) (+ (:max p) (:step p)) (:step p))]
                        (radio :text (str n) :group group :selected? (= n d))))]
    [panel group]))

(defn make-discreet-event-widget
  [p]
  (match [(:min p) (:max p) (:step p)]
         [0 1 1] [:checkbox (checkbox :selected? (= (:default p) 1)) nil]
         :else (concat [:radio] (make-radio-event-widget p))))

(defn make-switches-panel
  [events]
  (let [items (for [[n p] events]
                (border-panel
                 :north (label :text (str n ": " (:name p)) :class :event-title)
                 :center (let [[typ t g] (make-discreet-event-widget p)]
                           (swap! mono-player-events assoc (:symbol p) {:type typ :widget t :group g})
                           t)))]
    (grid-panel :rows 1 :columns (count items) :border "" :items items)))

(defn make-main-panel
  ([cc-events] (make-main-panel cc-events nil))
  ([cc-events pc-switches]
     (let [discreet-cc-events (filter #(= :discreet (:type (val %))) cc-events)
           continuous-cc-events (filter #(not= :discreet (:type (val %))) cc-events)]
       (border-panel :north (make-switches-panel discreet-cc-events)
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
                    :checkbox (config! (:widget e) :selected? (= val 1))
                    :radio (doseq [b (config (:group e) :buttons)]
                             (let [s (str (:val val))
                                   t (text b)]
                               (println s " " t)
                               (config! b :selected? (= s t)))))))
              (concat event-key [:mono-midi-player-event]))
    (apply-stylesheet f {[:.event-title] {:font (font :style :bold)}})
    (invoke-later
     (-> f pack! show!))
    f))

;(monitor wx7mooger wx7mooger-midi-map "wx7mooger")
