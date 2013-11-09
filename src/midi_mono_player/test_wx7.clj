(ns midi-mono-player.test-wx7
  (:use [overtone.live]
        [midi-mono-player player profiles]))

(definst wx7tb303
  [note       {:default 60 :min 0 :max 120 :step 1}
   wave       {:default 1 :min 0 :max 2 :step 1}
   r          {:default 0.8 :min 0.01 :max 0.99 :step 0.01}
   attack     {:default 0.0001 :min 0.001 :max 4 :step 0.001}
   decay      {:default 0.3 :min 0.001 :max 4 :step 0.001}
   sustain    {:default 0.99 :min 0.001 :max 0.99 :step 0.001}
   release    {:default 0.0001 :min 0.001 :max 4 :step 0.001}
   cutoff     {:default 500 :min 500 :max 2000 :step 1}
   env-amount {:default 0.01 :min 0.001 :max 4 :step 0.001}
   amp        {:default 0.5 :min 0 :max 1 :step 0.01}]
  (let [freq       (midicps note)
        freqs      [freq (* 1.01 freq)]
        vol-env    (env-gen (adsr attack decay sustain release)
                                        ;(line:kr 1 0 (+ attack decay release))
                            1
                            :action FREE)
        fil-env    (env-gen (perc))
        fil-cutoff (+ cutoff (* env-amount fil-env))
        waves      (* vol-env
                      [(saw freqs)
                       (pulse freqs 0.5)
                       (lf-tri freqs)])
        selector   (select wave waves)
        filt       (rlpf selector fil-cutoff r)]
    (* amp filt 4)))

(def wx7tb303-midi-map {:control-change [[2 "amp" :continuous]
                                         [51 "wave" :discreet]
                                         [27 "cutoff" :continuous]]})

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

(def wx7mooger-midi-map {:control-change [[2 "amp" :continuous]
                               [27 "width" :continuous]
                               [7 "cutoff" :continuous]
                               [51, "osc2-level" :discreet]
                               [52, "osc1" :discreet]
                               [53, "osc2" :discreet]]
                         :program-change []})


(definst ding
  [note {:default 60 :min 0 :max 127 :step 1}
   amp  {:default 0.3 :min 0 :max 1.0 :step 0.01}
   boost {:default 2 :min 1 :max 20 :step 1}
   level {:default 0.05 :min 0.05 :max 0.5 :step 0.05}
   gate 1]
  (let [freq (midicps note)
        snd  (sin-osc freq)
        ;;dist (distort (* boost (clip2 snd level)))
        env  (env-gen (adsr 0.5 0.1 0.99 0.001) gate :action FREE)]
    (* amp env snd)))

(def ding-midi-map {:control-change
                    [[2 "amp" :continuous]
                     [27 "level" :continuous]
                     [7 "boost" :continuous]]})


(definst foo
  [note {:default 48 :min 0 :max :127 :step 1}
   width {:default 0.5 :min 0.1 :max 0.9 :step 0.1}
   amp {:default 0.3 :min 0 :max 1 :step 0.01}
   gate 1]
  (let [freq (midicps note)
        snd  (pulse freq width)
        env  (env-gen (adsr 0.001 0.1 0.99 0.001) gate :action FREE)
        ]
    (* amp env snd)))


(definst wx7saw-synth-6
  "a detuned and stereo-separated saw synth with a low-pass-filter and
   low-pass-filter LFO."
  [note                {:default 48 :min 0 :max :127 :step 1}
   amp                 {:default 0.3 :min 0 :max 1 :step 0.01}
   lpf-res             {:default 0.1  :min 0.0 :max 1.0   :step 0.05}
   separation-delay-ms {:default 15.0  :min 0    :max 30.0  :step 5.0}
   cutoff              {:default 3000 :min 1000 :max 4000 :step 1000}
   gate                1]
  (let [pitch-freq (midicps note)
        saws-out (mix (saw [pitch-freq (+ pitch-freq (/ pitch-freq 440))]))
        separation-delay (/ separation-delay-ms 1000.0)
        saws-out-2ch [saws-out (delay-c saws-out 1.0 separation-delay)]
        lpf-out-2ch (moog-ff saws-out-2ch cutoff lpf-res)
        verbed (free-verb lpf-out-2ch 0.5 0.5 0.5)
        ]
    (* amp verbed)))

(def wx7saw-synth-6-midi-map {:control-change [[2 "amp" :continuous]
                                               [51 "separation-delay-ms" :discreet]
                                               [52 "cutoff" :discreet]
                                               [27 "lpf-res" :continuous]
                                               [7 "detune-level" :continuous]]})

(definst wx7additive
  "additive synth"
  [note {:default 48 :min 0 :max 127 :step 1}
   amp  {:default 0.3 :min 0 :max 1 :step 0.01}
   osc1-level {:default 1 :min 0 :max 1 :step 0.2}
   osc2-level {:default 0.6 :min 0 :max 1 :step 0.2}
   osc3-level {:default 0.4 :min 0 :max 1 :step 0.2}
   osc4-level {:default 0.2 :min 0 :max 1 :step 0.2}
   osc5-level {:default 0.2 :min 0 :max 1 :step 0.2}
   osc6-level {:default 0.2 :min 0 :max 1 :step 0.2}
   gate 1]
  (let [freq       (midicps note)
        levels     [osc1-level osc2-level osc3-level osc4-level osc5-level osc6-level]
        harmonics  (range 1 6)
        comp       (fn [harmonic level]
                     (* 1/2 amp level (sin-osc (* harmonic freq))))
        sig        (mix (map comp harmonics levels))
        ]
    sig))

(def wx7additive-midi-map {:control-change [[2 "amp" :continuous]
                                            [51 "osc1-level" :discreet]
                                            [52 "osc2-level" :discreet]
                                            [53 "osc3-level" :discreet]
                                            [54 "osc4-level" :discreet]
                                            [55 "osc5-level" :discreet]
                                            [56 "osc6-level" :discreet]
                                            ]})


;(def player (play wx7mooger (:wx7 profile-map) wx7mooger-midi-map))

(defn pctl [p c v]
  (ctl (:synth p) c v))

"
(on-event [:midi :program-change]
          (fn [e]
            (let [note (:note e)
                  vel  (:velocity e)]
              (println note " " vel)))
          ::pc-handler)
"
