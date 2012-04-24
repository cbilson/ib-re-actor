(defproject ib-re-actor "0.1.0-SNAPSHOT"
  :description "Clojure friendly wrapper for InteractiveBrokers java API"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [clj-time "0.4.0"]
                 ;; download this from IB and add it to your local m2
                 ;; repo, as per leiningen's instructions
                 [jtsclient "9.6.5"]]
  :dev-dependencies [[midje "1.3.1"]
                     [lein-exec "0.1"]
                     [lein-marginalia "0.6.1"]]
  :plugins [[lein-midje "2.0.0-SNAPSHOT"]])
