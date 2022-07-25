(ns midi-mono-player.core
  (:use [midi-mono-player player test-wx7 monitor profiles program test-programs]))

(defn wx7run
  ([] (wx7run "wx7"))
  ([profile]
   ;(play wx7mooger (get profile-map (keyword profile)) wx7mooger-midi-map)
   ;(monitor wx7mooger wx7mooger-midi-map "wx7mooger")
   (do-programs program-map :wx7saw-synth profile-map)
   ))
