(ns midi-mono-player.program
  (:use [midi-mono-player player monitor]
        [overtone.core])
  (:require [overtone.libs.event :as e]))

(defn kill-program
  "sends a kill event. The intent is that the running program will get the event and die"
  []
  (e/event [:mono-midi-program-event] {:type :kill}))

(defn do-programs
  "accepts a program map and an initial selection and a profile map. Runs
the selected instrument with the parameters given by the program map and
the profile specified in the program map (see test-programs, test-wx7 and profiles
for examples).
"
  ([progs sel profiles] (do-programs progs sel profiles true))
  ([progs sel profiles monitor?]
     (let [p (get progs sel)
           player*   (atom (play (:inst p) (get profiles (:profile p)) (:midi-map p)))
           mon (if monitor? (monitor (:inst p) (:midi-map p) (:name p) kill-program))]
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
                   [::program :mono-midi-program-event]))))
