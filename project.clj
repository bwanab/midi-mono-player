(defproject midi-mono-player "0.1.0-SNAPSHOT"
  :description "a mono midi player that responds to control-change events, program change and pitch-bend"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [overtone "0.9.0-SNAPSHOT"]
                 [seesaw "1.4.4"]]
  :main midi-mono-player.core)
