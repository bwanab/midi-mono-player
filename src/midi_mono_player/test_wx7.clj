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
                               [7 "cutoff" :continuous]
                               [27 "width" :continuous]
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
        env  (env-gen (adsr 0.001 0.1 0.99 0.001) gate :action FREE)]
    (* amp env snd)))

(def ding-midi-map {:control-change
                    [[2 "amp" :continuous]
                     [7 "boost" :continuous]
                     [27 "level" :continuous]]})

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
   lpf-lfo-freq        {:default 4.1  :min 0.0 :max 10.0  :step 0.01}
   lpf-min-freq        {:default 400  :min 400 :max 9900  :step 400}
   lpf-max-freq        {:default 4000 :min 100 :max 10000 :step 100}
   lpf-res             {:default 0.1  :min 0.0 :max 1.0   :step 0.05}
   separation-delay-ms {:default 5.0  :min 0    :max 30.0  :step 5.0}
   lfo-level           {:default 1.4  :min 0.0 :max 5.0   :step 0.05}
   lfo-freq            {:default 1.8  :min 0.0 :max 10.0  :step 0.1}
   pitch-index         {:default 0    :min 0   :max 15    :step 1}
   adsr-attack-time    {:default 0.001 :min 0.0  :max 1.0   :step 0.01}
   adsr-decay-time     {:default 0.3 :min 0.0  :max 1.0   :step 0.01}
   adsr-sustain-level  {:default 0.99 :min 0.0  :max 1.0   :step 0.01}
   adsr-release-time   {:default 0.001 :min 0.0  :max 1.0   :step 0.01}
   adsr-peak-level     {:default 0.9 :min 0.0  :max 1.0   :step 0.01}
   adsr-curve          {:default -4  :min -5   :max 5     :step 1}
   gate                {:default 1.0 :min 0.0  :max 1.0   :step 1}]
  (let [pitch-freq (midicps note)
        lfo-out (* lfo-level (sin-osc lfo-freq))
        saws-out (mix (saw [pitch-freq (+ pitch-freq lfo-out)]))
        separation-delay (/ separation-delay-ms 1000.0)
        saws-out-2ch [saws-out (delay-c saws-out 1.0 separation-delay)]
        lpf-freq (lin-lin (sin-osc lpf-lfo-freq) -1 1 lpf-min-freq lpf-max-freq)
        lpf-out-2ch (moog-ff saws-out-2ch lpf-freq lpf-res)
        env-out (env-gen (adsr adsr-attack-time   adsr-decay-time
                               adsr-sustain-level adsr-release-time
                               adsr-peak-level    adsr-curve)
                         :gate gate
                         :action FREE)
        ]
    (* amp env-out lpf-out-2ch)))

(def wx7saw-synth-6-midi-map {:control-change [[2 "amp" :continuous]
                                               [51 "separation-delay-ms" :discreet]
                                               [27 "lfo-freq" :continuous]
                                               [7 "lfo-level" :continuous]]})

(definst wx7additive
  "additive synth"
  [note {:default 48 :min 0 :max 127 :step 1}
   amp  {:default 0.3 :min 0 :max 1 :step 0.01}
   osc1-level {:default 0.4 :min 0 :max 1 :step 0.2}
   osc2-level {:default 0.4 :min 0 :max 1 :step 0.2}
   osc3-level {:default 0.4 :min 0 :max 1 :step 0.2}
   osc4-level {:default 0.4 :min 0 :max 1 :step 0.2}
   osc5-level {:default 0.4 :min 0 :max 1 :step 0.2}
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
        amp-env    (env-gen (adsr attack decay sustain release) gate :action FREE)
        f-env      (env-gen (adsr fattack fdecay fsustain frelease) gate)
        p8         (* 2.0 freq)
        p82        (* 2.0 p8)
        s1         (* osc1-level (sin-osc freq))
        s2         (* osc2-level (sin-osc p8))
        s3         (* osc3-level (sin-osc (* 1.5 p8)))
        s4         (* osc4-level (sin-osc p82))
        s5         (* osc5-level (sin-osc (* 1.25 p82)))
        sig        (+ s1 s2 s3 s4 s5)
        ;; filt       (moog-ff sig (* cutoff f-env) 3)
        ]
    (* amp sig 2)))

(def wx7additive-midi-map {:control-change [[2 "amp" :continuous]
                                            [51 "osc1-level" :discreet]
                                            [52 "osc2-level" :discreet]
                                            [53 "osc3-level" :discreet]
                                            [54 "osc4-level" :discreet]
                                            [55 "osc5-level" :discreet]
                                            [27 "cutoff" :continuous]]})


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
