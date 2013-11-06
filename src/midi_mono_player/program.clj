(ns midi-mono-player.program
  (:use [midi-mono-player player monitor]
        [overtone.core])
  (:require [overtone.libs.event :as e]))

(defn do-programs
  [progs sel profiles]
  (let [p (get progs sel)
        player*   (atom (play (:inst p) (get profiles (:profile p)) (:midi-map p)))
        mon (monitor (:inst p) (:midi-map p) (:name p) kill-program)]
    (e/on-event [:midi :program-change]
                (fn [{program :note dummy :velocity}]
                  (when-let [pp (get progs program)]
                    (kill (:synth @player*))
                    (reset! player* (play (:inst pp) (get profiles (:profile pp)) (:midi-map pp)))
                    (update-monitor mon (:inst pp) (:midi-map pp) (:name pp) kill-program)))
                [::midi-mono-player :midi :program-change])
    (e/on-event [:mono-midi-program-event]
                (fn [val]
                  (println ":mono-midi-program-event")
                  (kill (:synth @player*))
                  (kill-monitor mon))
                [::program :mono-midi-program-event])))

(defn kill-program
  []
  (e/event [:mono-midi-program-event] {:type :kill}))
