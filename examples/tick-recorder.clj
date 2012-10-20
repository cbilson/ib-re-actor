(ns ib-re-actor.examples.tick-recorder
  (:use [ib-re-actor.gateway]
        [clj-time.core :only [date-time minus now]]
        [clj-time.coerce :only [to-long]]
        [clojure.java.io]
        [clojure.tools.logging :only [debug warn error]]
        [clj-logging-config.log4j :only [set-logger!]]))

(def contracts
  [
   {:type :future :local-symbol "YM   DEC 12" :exchange "ECBOT"}
   ;; {:type :future :local-symbol "YM   MAR 13" :exchange "ECBOT"}
   ;; {:type :index :symbol "YM" :exchange "ECBOT"}
   ;; {:type :future :local-symbol "TFZ2" :exchange "NYBOT"}
   ;; {:type :future :local-symbol "TFH3" :exchange "NYBOT"}
   ;; {:type :index :symbol "TF" :exchange "NYBOT"}
   ;; {:type :future :local-symbol "ESZ2" :exchange "GLOBEX"}
   ;; {:type :future :local-symbol "ESH3" :exchange "GLOBEX"}
   ;; {:type :index :symbol "ES" :exchange "GLOBEX"}
   ;; {:type :future :local-symbol "NQZ2" :exchange "GLOBEX"}
   ;; {:type :future :local-symbol "NQH3" :exchange "GLOBEX"}
   ;; {:type :index :symbol "NQ" :exchange "GLOBEX"}
   ;; {:type :future :local-symbol "E7Z2" :exchange "GLOBEX"}
   ;; {:type :future :local-symbol "ZQ   DEC 12" :exchange "ECBOT"} ; 30-day fed funds
   ;; {:type :future :local-symbol "ZN   MAR 13" :exchange "ECBOT"} ; 10y treasury
   ;; {:type :index :symbol "TICK-NYSE" :exchange "NYSE"}
   ;; {:type :index :symbol "TRIN-NYSE" :exchange "NYSE"}
   ;; {:type :equity :symbol "XOM" :exchange "NYSE"}
   ;; {:type :equity :symbol "AAPL" :exchange "SMART" :currency "USD"}
   ;; {:type :equity :symbol "IBM" :exchange "NYSE" :currency "USD"}
   ;; {:type :equity :symbol "MSFT" :exchange "SMART" :currency "USD"}
   ;; {:type :equity :symbol "CVX" :exchange "NYSE" :currency "USD"}
   ;; {:type :equity :symbol "GE" :exchange "NYSE" :currency "USD"}
   ;; {:type :equity :symbol "T" :exchange "NYSE" :currency "USD"}
   ;; {:type :equity :symbol "PG" :exchange "NYSE" :currency "USD"}
   ;; {:type :equity :symbol "JNJ" :exchange "NYSE" :currency "USD"}
   ;; {:type :equity :symbol "PFE" :exchange "NYSE" :currency "USD"}
   ;; {:type :equity :symbol "SPY" :exchange "ARCA" :currency "USD"}
   ;; {:type :equity :symbol "SPYV" :exchange "ARCA" :currency "USD"}
   ;; {:type :equity :symbol "SPYG" :exchange "ARCA" :currency "USD"}
   ;; {:type :equity :symbol "IWM" :exchange "ARCA" :currency "USD"}
   ;; {:type :equity :symbol "IWN" :exchange "ARCA" :currency "USD"}
   ;; {:type :equity :symbol "IWO" :exchange "ARCA" :currency "USD"}
   ;; {:type :index :symbol "TICK-NASD" :exchange "NASDAQ"}
   ;; {:type :index :symbol "TRIN-NASD" :exchange "NASDAQ"}
   ])

(def ^:dynamic *done* (promise))
(def ^:dynamic *out-writer* nil)

(defmulti message-handler :type)

(defmethod message-handler :error
  [{:keys [request-id code message exception] :as msg}]
  (let [req (or request-id "???")]
    (cond
     (= 322 code) ; duplicate ticker id
     (debug "ignoring duplicate ticker id: " message)

     (contains? msg :message)
     (do
       (error "*** [" req "] " message)
       (when (not (warning? msg))
         (deliver *done* true)))

     (contains? msg :exception)
     (do
       (error "*** [" req "] " exception "\n")
       (when (not (warning? msg))
         (deliver *done* true))))))

(defmethod message-handler :tick [{:keys [contract field value]}]
  (debug "Got: " field " " value)
  (.write *out-writer*
          (str (now) ","
               symbol ","
               (:name field) "," val "\n")))

(defmethod message-handler :default [msg]
  (debug "??? unhandled message: " (prn-str msg)))

(defn record-ticks []
  (subscribe message-handler)
  (try
    (binding [*out-writer* (writer (file "ticks.csv") :append false)
              *done* (promise)]
      (doseq [contract contracts]
        (debug "Requesting " contract)
        (request-market-data contract))
      @*done*)
    (finally
      (unsubscribe message-handler)
      (when (not (nil? *out-writer*))
        (.close *out-writer*)))))

(comment
  (set-logger! :level :debug)
  (connect "localhost" 4001))
