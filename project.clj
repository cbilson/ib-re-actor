(defproject ib-re-actor "0.1.1-SNAPSHOT"
  :description "Clojure friendly wrapper for InteractiveBrokers java API"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-time "0.4.4"]
                 [com.ib/jtsclient "9.68"]
                 [org.clojure/tools.logging "0.2.3"]
                 [clj-logging-config "1.9.10"]]
  :repositories {"stuart" "http://stuartsierra.com/maven2"}
  :plugins [[lein-midje "2.0.1"]]
  :profiles {:dev {:dependencies [[midje "1.4.0"]
                                  [com.stuartsierra/lazytest "1.2.3"]]}})
