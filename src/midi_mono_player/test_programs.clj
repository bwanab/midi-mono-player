(ns midi-mono-player.test-programs
  (:use [midi-mono-player test-wx7 program profiles]))

(def program-map
  {
   0
   {
    :name "wx7mooger"
    :inst wx7mooger
    :midi-map wx7mooger-midi-map
    :profile :wx7
    }
   1
   {
    :name "foo"
    :inst foo
    :midi-map {:control-change [[2 "amp" :continuous]
                                [27 "width" :continuous]]}
    :profile :wx7
    }
   2
   {
    :name "ding"
    :inst ding
    :midi-map {:control-change [[2 "amp" :continuous]]}
    :profile :wx7
    }
   3
   {
    :name "wx7tb303"
    :inst wx7tb303
    :midi-map wx7tb303-midi-map
    :profile :wx7
    }
   4
   {
    :name "wx7saw-synth-6"
    :inst wx7saw-synth-6
    :midi-map wx7saw-synth-6-midi-map
    :profile :wx7
    }
   })

(do-programs program-map 0 profile-map)
