(ns midi-mono-player.core
  (:use [midi-mono-player player test-wx7 monitor profiles]))

(defn -main
  ([] (-main "wx7"))
  ([profile]
  (play wx7mooger (get profile-map (keyword profile)) fcb-map)
  (monitor wx7mooger fcb-map)))
