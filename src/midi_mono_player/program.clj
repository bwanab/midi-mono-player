(ns midi-mono-player.program
  (:use [midi-mono-player player monitor]
        [overtone.core])
  (:require [overtone.libs.event :as e]))

(defn do-programs
  [progs sel profiles]
  (let [p (get progs sel)
        player*   (atom (play (:inst p) (get profiles (:profile p)) (:midi-map p)))
        mon (monitor (:inst p) (:midi-map p))]
    (e/on-event [:midi :program-change]
                (fn [{program :note dummy :velocity}]
                  (when-let [pp (get progs program)]
                    (kill (:synth @player*))
                    (reset! player* (play (:inst p) (get profiles (:profile p)) (:midi-map p)))
                    (update mon (:inst pp) (:midi-map pp))))
                [::midi-mono-player :midi :program-change])))
