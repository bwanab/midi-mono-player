(ns midi-mono-player.test-programs
  (:use [midi-mono-player test-wx7 program profiles]))

(def program-map
  {
   0
   {
    :inst wx7mooger
    :midi-map wx7mooger-midi-map
    :profile :wx7
    }
   1
   {
    :inst foo
    :midi-map {:control-change [[2 "amp" :continuous]
                                [27 "width" :continuous]]}
    :profile :wx7
    }
   2
   {
    :inst ding
    :midi-map {:control-change [[2 "amp" :continuous]]}
    :profile :wx7
    }
   })

(do-programs program-map 1 profile-map)
