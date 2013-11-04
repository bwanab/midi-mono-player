(ns midi-mono-player.test-wx7
    (:use [overtone.live]
          [midi-mono-player.wx7]))

(definst wx7mooger
  "Choose 0, 1, or 2 for saw, sin, or pulse"
  [note {:default 48 :min 0 :max 127 :step 1}
   amp  {:default 0.3 :min 0 :max 1 :step 0.01}
   osc1 {:default 2 :min 0 :max 2 :step 1}
   osc2 {:default 1 :min 0 :max 2 :step 1}
   osc1-level {:default 0.5 :min 0 :max 1 :step 0.01}
   osc2-level {:default 0.0 :min 0 :max 1 :step 0.1}
   width {:default 0.5 :min 0.1 :max 0.5 :step 0.05}
   cutoff {:default 500 :min 500 :max 4000 :step 1}
   attack {:default 0.0001 :min 0.0001 :max 5 :step 0.001}
   decay {:default 0.3 :min 0.0001 :max 5 :step 0.001}
   sustain {:default 0.99 :min 0.0001 :max 1 :step 0.001}
   release {:default 0.0001 :min 0.0001 :max 6 :step 0.001}
   fattack {:default 0.0001 :min 0.0001 :max 6 :step 0.001}
   fdecay {:default 0.3 :min 0.0001 :max 6 :step 0.001}
   fsustain {:default 0.999 :min 0.0001 :max 1 :step 0.001}
   frelease {:default 0.0001 :min 0.0001 :max 6 :step 0.001}
   gate 1]
  (let [freq       (midicps note)
        osc-bank-1 [(saw freq) (sin-osc freq) (pulse freq width) ]
        osc-bank-2 [(saw freq) (sin-osc freq) (pulse freq width) ]
        amp-env    (env-gen (adsr attack decay sustain release) gate :action FREE)
        f-env      (env-gen (adsr fattack fdecay fsustain frelease) gate)
        s1         (* osc1-level (select osc1 osc-bank-1))
        s2         (* osc2-level (select osc2 osc-bank-2))
        filt       (moog-ff (+ s1 s2) (* cutoff f-env) 3)]
    (* amp filt 4)))

(definst ding
  [note 60 amp 0.3 gate 1]
  (let [freq (midicps note)
        snd  (sin-osc freq)
        env  (env-gen (adsr 0.001 0.1 0.99 0.001) gate :action FREE)]
    (* amp env snd)))

(definst foo
  [note {:default 48 :min 0 :max :127 :step 1}
   width {:default 0.5 :min 0.1 :max 0.9 :step 0.1}
   amp {:default 0.3 :min 0 :max 1 :step 0.01}
   gate 1]
  (let [freq (midicps note)
        snd (pulse freq width)
        env  (env-gen (adsr 0.001 0.1 0.99 0.001) gate :action FREE)]
    (* amp env snd)))

(def fcb-map {:control-change [[2 "amp" :continuous]
                               [7 "cutoff" :continuous]
                               [27 "width" :continuous]
                               [51, "osc2-level" :discreet]]
              :program-change [[0 "osc1"]
                               [1 "osc2"]]})
(def player (yamaha-wx7 wx7mooger fcb-map))

(defn pctl [p c v]
  (ctl (:synth (val (first p))) c v))

"
(on-event [:midi :program-change]
          (fn [e]
            (let [note (:note e)
                  vel  (:velocity e)]
              (println note " " vel)))
          ::pc-handler)
"
