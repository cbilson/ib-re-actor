(defproject re-actor "0.0.1-SNAPSHOT"
  :description "Clojure friendly wrapper for InteractiveBrokers java API"
  :dependencies [[org.clojure/clojure "1.2.1"]
                 [clj-time "0.3.0"]
                 ;; download this from IB and add it to your local m2
                 ;; repo, as per leiningen's instructions
                 [jtsclient "9.6.5"]]
    :dev-dependencies [[midje "1.3-alpha2"]])
