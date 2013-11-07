# midi-mono-player

An overtone library to support playing of instruments with monophonic input devices such as
wind controllers. Honestly, it is Specifically to support my Yamaha WX7 and my Behringer FCB-1010
foot pedal, but it should be generic enough to work with other controllers. The player handles
pitch-bend and breath-control midi events and allows for control of others.

In addition, the library provides a gui monitor that shows the state of the currently selected instrument
and its parameters.

This library is designed for live performance.

It allows for

1. The specification of a group of Overtone insts that are available.
2. The definitions of the midi controllers that will control the parameters for those insts.


It uses standard Overtone insts with the caveats that
1. The inputs must have metadata defined.
2. The gate parameter for envelope should be 1.
3. "note" and "amp" parameters should be given.

## Usage

Input device specific values are stored in profiles.clj. Currently, this file has only
device specific values for the Yamaha WX7.

Instrument definitions can be stored anywhere that they are available in the classpath. A sample set of simple
instruments is given in the namespace test-wx7.

Let's look at a sample instrument and its midi specification (this is a copy of overtone.inst.synt/mooger
with the addition of a width parameter to control pulse width of the two ocillators when the pulse generator
is selected:

```clj
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
                               [53, "osc2" :discreet]]})
```
It's worth noting that my FCB-1010 has two foot pedals that generate control-change events 7 and 27
with values from 0 to 127. The wx7mooger-midi-map defines these two pedals to control cutoff and width
respectively. The player automatically computes the min/max values for those parameters as defined
in the instrument and scales the events to span the min/max range. Channel 2 is breath-control which
is assigned to amp. Note that these are all specified as continuous events.

The two ocillators can have saw, sin, or pulse by setting the osc1/2 values to 1,2,3 respectively. Since,
the values are defined as discreet, each event will cycle to the next value and back to the min when the
current value is max. On my FCB-1010, I have a foot button that sends control-change events on these channels.

Any number of instruments can be defined like this.

A program is a definition of a set of instruments and their midi mappings. An example is given in the
namespace test-programs. Let's look at it here:

```clj
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
```

This program map defines 5 different instruments along with a midi-map and a profile for each. For the
foo and ding instruments, the midi-maps are defined in-line. The keys to the map are the midi program
change events that will trigger its instrument to be the current instrument.

In performance, it's generally nice to have feedback on what you're playing. Namespace monitor in this
project provides a simple GUI that shows the currently selected instrument as well as its parameters and
their current setting.



## License

Copyright © 2013 FIXME

Distributed under the Eclipse Public License, the same as Clojure.
