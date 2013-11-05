(ns midi-mono-player.profiles)

"
central-bend-point is the value of :velocity in a pitch-bench event that is most common with normal emboucher.
This is an observed value on my wx7.

The distribution of values of :velocity is a rough bell curve around the central point with min values of 68
and max values of 90. I want a bend of about a demi-tone in each direction. This computes to a -0.1 factor for each
value below 78 and +0.1 for each value above.

"


(def profile-map
  {:wx7
   {
    :device-key [:midi]
    :player-key ::yamaha-wx7
    :central-bend-point 78.0
    :bend-factor 0.1}
   })
