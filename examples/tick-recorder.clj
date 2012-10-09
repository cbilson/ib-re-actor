(ns ib-re-actor.examples.tick-recorder
  (:use [ib-re-actor.gateway]
        [clj-time.core :only [date-time minus now]]
        [clj-time.coerce :only [to-long]]
        [clojure.java.io]))

(def contracts
  {
   1 {:type :future :local-symbol "YM   DEC 12" :exchange "ECBOT"}
   2 {:type :future :local-symbol "YM   MAR 13" :exchange "ECBOT"}
   3 {:type :index :symbol "YM" :exchange "ECBOT"}
   4 {:type :future :local-symbol "TFZ2" :exchange "NYBOT"}
   5 {:type :future :local-symbol "TFH3" :exchange "NYBOT"}
   6 {:type :index :symbol "TF" :exchange "NYBOT"}
   7 {:type :future :local-symbol "ESZ2" :exchange "GLOBEX"}
   8 {:type :future :local-symbol "ESH3" :exchange "GLOBEX"}
   9 {:type :index :symbol "ES" :exchange "GLOBEX"}
   10 {:type :future :local-symbol "NQZ2" :exchange "GLOBEX"}
   11 {:type :future :local-symbol "NQH3" :exchange "GLOBEX"}
   12 {:type :index :symbol "NQ" :exchange "GLOBEX"}
   13 {:type :future :local-symbol "E7Z2" :exchange "GLOBEX"}
   14 {:type :future :local-symbol "ZQ   DEC 12" :exchange "ECBOT"} ; 30-day fed funds
   15 {:type :future :local-symbol "ZN   MAR 13" :exchange "ECBOT"} ; 10y treasury
   16 {:type :index :symbol "TICK-NYSE" :exchange "NYSE"}
   17 {:type :index :symbol "TRIN-NYSE" :exchange "NYSE"}
   18 {:type :equity :symbol "XOM" :exchange "NYSE"}
   19 {:type :equity :symbol "AAPL" :exchange "SMART" :currency "USD"}
   20 {:type :equity :symbol "IBM" :exchange "NYSE" :currency "USD"}
   21 {:type :equity :symbol "MSFT" :exchange "SMART" :currency "USD"}
   22 {:type :equity :symbol "CVX" :exchange "NYSE" :currency "USD"}
   23 {:type :equity :symbol "GE" :exchange "NYSE" :currency "USD"}
   24 {:type :equity :symbol "T" :exchange "NYSE" :currency "USD"}
   25 {:type :equity :symbol "PG" :exchange "NYSE" :currency "USD"}
   26 {:type :equity :symbol "JNJ" :exchange "NYSE" :currency "USD"}
   27 {:type :equity :symbol "PFE" :exchange "NYSE" :currency "USD"}
   28 {:type :equity :symbol "SPY" :exchange "ARCA" :currency "USD"}
   29 {:type :equity :symbol "SPYV" :exchange "ARCA" :currency "USD"}
   30 {:type :equity :symbol "SPYG" :exchange "ARCA" :currency "USD"}
   31 {:type :equity :symbol "IWM" :exchange "ARCA" :currency "USD"}
   32 {:type :equity :symbol "IWN" :exchange "ARCA" :currency "USD"}
   33 {:type :equity :symbol "IWO" :exchange "ARCA" :currency "USD"}
   34 {:type :index :symbol "TICK-NASD" :exchange "NASDAQ"}
   35 {:type :index :symbol "TRIN-NASD" :exchange "NASDAQ"}
   })

(def done (promise))

(def log-agent (agent *out*))

(defn log-tick [writer received ticker-id field val]
  (let [contract (get contracts ticker-id)
        symbol (if (= :future (:type contract))
                 (:local-symbol contract)
                 (:symbol contract))]
    (.write writer (str (to-long received) "," symbol "," (name field) "," val "\n"))
    writer))

(def error-agent (agent *err*))

(defn log-error [writer message]
  (.write writer (str (now) message))
  writer)

(defmulti message-handler :type)

(defmethod message-handler :error [{:keys [request-id message exception] :as msg}]
  (let [req (or request-id "???")]
    (cond
     (contains? msg :message) (send-off error-agent log-error (str "*** [" req "] " message "\n"))
     (contains? msg :exception) (send-off error-agent log-error (str "*** [" req "] " (.toString exception) "\n")))
    (if (not (warning? msg))
      (do
        (await error-agent log-agent)
        (deliver done true)))))

(defmethod message-handler :tick [{:keys [ticker-id field value]}]
  (send-off log-agent log-tick (now) ticker-id field value))

(defmethod message-handler :timestamp-tick [_])

(defmethod message-handler :managed-accounts [_])

(defmethod message-handler :next-valid-order-id [_])

(defmethod message-handler :default [msg]
  (send-off error-agent log-error (str "??? unhandled message: " (prn-str msg))))

(defn agent-errs []
  (filter identity (map agent-errors [log-agent])))

(defn record-ticks []
  (with-open [log-writer (writer (file "ticks.csv") :append false)
              error-writer (writer (file "errors.log") :append false)]
    (send-off log-agent (fn [_] log-writer))
    (send-off error-agent (fn [_] error-writer))
    (with-open-connection [connection (connect message-handler)]
      (doseq [[ticker-id contract] contracts]
        (request-market-data connection ticker-id contract))
      @done)))
