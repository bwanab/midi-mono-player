(ns midi-mono-player.test-wx7
  (:use [overtone.live]
        [overtone.examples.compositions.bells]
        [midi-mono-player player profiles]))

(definst bell
  [note       {:default 60 :min 0 :max 120 :step 1}
   amp        {:default 0.5 :min 0 :max 1 :step 0.01}
   dur        {:default 1.0 :min 1 :max 7.0 :step 1}]
  (let [freq (midicps note)
        snd (* amp (bell-partials freq dur partials))]
    snd))

(def bell-midi-map {:control-change
                    [[2 "amp" :continuous]
                     [27 "dur" :continuous]]})

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
   pwidth1 {:default 0.5 :min 0.1 :max 0.5 :step 0.1}
   pwidth2 {:default 0.5 :min 0.1 :max 0.5 :step 0.1}
   cutoff {:default 500 :min 500 :max 4000 :step 1}
   resonance {:default 3 :min 0 :max 4 :step 1}
   fattack {:default 0.0001 :min 0.0001 :max 6 :step 0.001}
   fdecay {:default 0.3 :min 0.0001 :max 6 :step 0.001}
   fsustain {:default 0.999 :min 0.0001 :max 1 :step 0.001}
   frelease {:default 0.0001 :min 0.0001 :max 6 :step 0.001}
   gate 1]
  (let [freq       (midicps note)
        osc-bank-1 [(saw freq) (sin-osc freq) (pulse freq pwidth1) ]
        osc-bank-2 [(saw freq) (sin-osc freq) (pulse freq pwidth2) ]
        f-env      (env-gen (adsr fattack fdecay fsustain frelease) gate)
        s1         (* osc1-level (select osc1 osc-bank-1))
        s2         (* osc2-level (select osc2 osc-bank-2))
        filt       (moog-ff [s1 s2] (* cutoff f-env) resonance)]
    (* amp filt)))

(def wx7mooger-midi-map {:control-change [[2 "amp" :continuous]
                               [7 "cutoff" :continuous]
                               [27 "resonance" :continuous]
                               [51, "osc2-level" :discreet]
                               [52, "osc1" :discreet]
                               [53, "osc2" :discreet]
                               [54 "pwidth1" :discreet]
                               [55 "pwidth2" :discreet]
                               ]
                         :program-change []})

(definst wx7nolpf
  "Choose 0, 1, or 2 for saw, sin, or pulse"
  [note {:default 48 :min 0 :max 127 :step 1}
   amp  {:default 0.3 :min 0 :max 1 :step 0.01}
   osc1 {:default 2 :min 0 :max 2 :step 1}
   osc2 {:default 1 :min 0 :max 2 :step 1}
   osc1-level {:default 0.5 :min 0 :max 1 :step 0.01}
   osc2-level {:default 0.5 :min 0 :max 1 :step 0.1}
   osc2-harmonic {:default 2 :min 1 :max 8 :step 1}
   pwidth1 {:default 0.5 :min 0.1 :max 0.5 :step 0.1}
   pwidth2 {:default 0.5 :min 0.1 :max 0.5 :step 0.1}
   bpf-freq {:default 100 :min 100 :max 4000 :step 1}
   bpf-q   {:default 1 :min 1 :max 4 :step 1}
   fattack {:default 0.0001 :min 0.0001 :max 6 :step 0.001}
   fdecay {:default 0.3 :min 0.0001 :max 6 :step 0.001}
   fsustain {:default 0.999 :min 0.0001 :max 1 :step 0.001}
   frelease {:default 0.0001 :min 0.0001 :max 6 :step 0.001}
   gate 1]
  (let [freq       (midicps note)
        freq2      (/ freq 2)
        osc-bank-1 [(saw freq) (sin-osc freq) (pulse freq pwidth1) ]
        osc-bank-2 [(saw freq2) (sin-osc freq2) (pulse freq2 pwidth2) ]
        f-env      (env-gen (adsr fattack fdecay fsustain frelease) gate)
        s1         (* osc1-level (select osc1 osc-bank-1))
        s2         (* osc2-level (select osc2 osc-bank-2))
        filt       (bpf (+ s1 s2) bpf-freq bpf-q)
        ]
    (* amp filt)))

(def wx7nolpf-midi-map {:control-change [[2 "amp" :continuous]
                               [7 "bpf-freq" :continuous]
                               [51, "osc2-level" :discreet]
                               [52, "osc1" :discreet]
                               [53, "osc2" :discreet]
                               [54 "pwidth1" :discreet]
                               [55 "pwidth2" :discreet]
                               [56 "bpf-q" :discreet]
                               ]
                         :program-change []})


(definst ding
  [note {:default 60 :min 0 :max 127 :step 1}
   amp  {:default 0.3 :min 0 :max 1.0 :step 0.01}
   boost {:default 4 :min 1 :max 20 :step 1}
   level {:default 0.01 :min 0.01 :max 0.5 :step 0.05}
   gate 1]
  (let [freq (midicps note)
        snd  (sin-osc freq)
        dist (distort (* boost (clip2 snd level)))
        ]
    (* amp dist)))

(def ding-midi-map {:control-change
                    [[2 "amp" :continuous]
                     [27 "level" :continuous]
                     [7 "boost" :continuous]]})

(defsynth ding-s
  [obus 0
   note {:default 60 :min 0 :max 127 :step 1}
   amp  {:default 0.3 :min 0 :max 1.0 :step 0.01}
   boost {:default 4 :min 1 :max 20 :step 1}
   level {:default 0.01 :min 0.01 :max 0.5 :step 0.05}
   gate 1]  (let [freq (midicps note)
        snd  (sin-osc freq)
        dist (distort (* boost (clip2 snd level)))
        ]
    (out obus (* amp dist))))

(def ding-s-midi-map {:control-change
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


(definst wx7saw-synth
  "a detuned and stereo-separated saw synth with a moog-filter.
   The detune is based on a percentage of the frequency hard-coded
   to 1/440 which is observed to sound good across the frequency range."
  [note                {:default 48 :min 0 :max :127 :step 1}
   amp                 {:default 0.3 :min 0 :max 1 :step 0.01}
   lpf-res             {:default 0.1  :min 0.0 :max 1.0   :step 0.05}
   separation-delay-ms {:default 15.0  :min 0    :max 30.0  :step 5.0}
   cutoff              {:default 3000 :min 1000 :max 4000 :step 1000}
   reverb-wet-dry      {:default 0.4 :min 0 :max 1.0 :step 0.2}
   reverb-room-size    {:default 0.4 :min 0 :max 1.0 :step 0.2}
   reverb-dampening    {:default 0.4 :min 0 :max 1.0 :step 0.2}
   gate                1]
  (let [pitch-freq (midicps note)
        pitch-freq2 (/ pitch-freq 2)
        saws-out (mix (saw [pitch-freq (+ pitch-freq2 (/ pitch-freq2 440))]))
        separation-delay (/ separation-delay-ms 1000.0)
        saws-out-2ch [saws-out (delay-c saws-out 1.0 separation-delay)]
        lpf-out-2ch (moog-ff saws-out-2ch cutoff lpf-res)
        verbed (free-verb lpf-out-2ch reverb-wet-dry reverb-room-size reverb-dampening)
        ]
    (* amp verbed)))

(def wx7saw-synth-midi-map {:control-change [[2 "amp" :continuous]
                                               [51 "separation-delay-ms" :discreet]
                                               [52 "cutoff" :discreet]
                                               [53 "reverb-wet-dry" :discreet]
                                               [54 "reverb-room-size" :discreet]
                                               [55 "reverb-dampening" :discreet]
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

(defsynth violin
  "violin inspired by Sound On Sound April-July 2003 articles."
  [note {:default 60 :min 0 :max 127 :step 1}
   amp {:default 1.0 :min 0.0 :max 1.0 :step 0.01}
   v-depth {:default 0.02 :min 0.01 :max 0.04 :step 0.005}
   v-rate {:default 6 :min 2 :max 10 :step 2}
   lpf-freq {:default 4000 :min 500 :max 10000 :step 1000}
   hpf-freq {:default 30 :min 0 :max 300 :step 30}
   gate 1
   out-bus 0]
  (let [freq (midicps note)
        ;; 3b) portamento to change frequency slowly
        ;;freqp (slew:kr freq 180.0 180.0)
        freqp freq
        ;; 3a) vibrato to make it seem "real"
        freqv (vibrato :freq freqp :rate v-rate :depth v-depth :delay 1)
        ;; 1) the main osc for the violin
        saw (saw freqv)
        ;; 2) add an envelope for "bowing"
        saw0 (* saw (env-gen (adsr 1.5 1.5 0.8 1.5) :gate gate :action FREE))
        ;; a low-pass filter prior to our filter bank
        saw1 (lpf saw0 lpf-freq) ;; freq???
        ;; 4) the "formant" filters
        band1 (bpf saw1 300 (/ 3.5))
        band2 (bpf saw1 700 (/ 3.5))
        band3 (bpf saw1 3000 (/ 2))
        saw2 (+ band1 band2 band3)
        ;; a high-pass filter on the way out
        saw3 (hpf saw2 hpf-freq) ;; freq???
        ]
    (out out-bus (pan2 (* amp  [saw3 saw3])))))

(def violin-midi-map {:control-change [[2 "amp" :continuous]
                                       [27 "lpf-freq" :continuous]
                                       [51 "v-depth" :discreet]
                                       [52 "v-rate" :discreet]
                                       [53 "hpf-freq" :discreet]
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
