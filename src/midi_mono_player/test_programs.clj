(ns midi-mono-player.test-programs
  (:use [midi-mono-player test-wx7 program profiles]))

(def program-map
  {
   :wx7mooger
   {
    :name "wx7mooger"
    :inst wx7mooger
    :midi-map wx7mooger-midi-map
    :profile :wx7
    }
   :foo
   {
    :name "foo"
    :inst foo
    :midi-map {:control-change [[2 "amp" :continuous]
                                [27 "width" :continuous]]}
    :profile :wx7
    }
   :ding
   {
    :name "ding"
    :inst ding
    :midi-map ding-midi-map
    :profile :wx7
    }
   :wx7tb303
   {
    :name "wx7tb303"
    :inst wx7tb303
    :midi-map wx7tb303-midi-map
    :profile :wx7
    }
   :wx7saw-synth
   {
    :name "wx7saw-synth"
    :inst wx7saw-synth
    :midi-map wx7saw-synth-midi-map
    :profile :wx7
    }
   :wx7additive
   {
    :name "wx7additive"
    :inst wx7additive
    :midi-map wx7additive-midi-map
    :profile :wx7
    }
   :wx7nolpf
   {
    :name "wx7nolpf"
    :inst wx7nolpf
    :midi-map wx7nolpf-midi-map
    :profile :wx7
    }
   :ding-s
   {
    :name "ding-s"
    :inst ding-s
    :midi-map ding-s-midi-map
    :profile :wx7
    }
   :violin
   {
    :name "violin"
    :inst violin
    :midi-map violin-midi-map
    :profile :wx7
    }
   })

(kill-program)
(Thread/sleep 2000)
;;(do-programs program-map 3 profile-map)
