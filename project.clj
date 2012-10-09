(defproject ib-re-actor "0.1.0-SNAPSHOT"
  :description "Clojure friendly wrapper for InteractiveBrokers java API"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-time "0.4.4"]
                 [com.ib/jtsclient "9.68"]]
  :dev-dependencies [[midje "1.3.1"]
                     [lein-exec "0.1"]
                     [lein-marginalia "0.6.1"]]
  :plugins [[lein-midje "2.0.0-SNAPSHOT"]])
