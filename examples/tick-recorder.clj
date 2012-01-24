(ns ib-re-actor.examples.tick-recorder
  (:use [ib-re-actor.connection]
        [ib-re-actor.contracts]
        [clj-time.core :only [date-time minus now]]
        [clj-time.coerce :only [to-long]]
        [clojure.java.io]))

(def contracts
  {
   1 (futures-contract "YM   MAR 12" "ECBOT")
   2 (futures-contract "YM   JUN 12" "ECBOT")
   3 (index "YM" "ECBOT")
   4 (futures-contract "TFH2" "NYBOT")
   5 (futures-contract "TFM2" "NYBOT")
   6 (index "TF" "NYBOT")
   7 (futures-contract "ESH2" "GLOBEX")
   8 (futures-contract "ESM2" "GLOBEX")
   9 (index "ES" "GLOBEX")
   10 (futures-contract "NQH2" "GLOBEX")
   11 (futures-contract "NQM2" "GLOBEX")
   12 (index "NQ" "GLOBEX")
   13 (futures-contract "E7H2" "GLOBEX")
   14 (futures-contract "ZQ   MAR 12" "ECBOT")
   15 (futures-contract "ZN   MAR 12" "ECBOT")
   16 (index "TICK-NYSE" "NYSE")
   17 (index "TRIN-NYSE" "NYSE")
   18 (equity "XOM" "NYSE")
   19 (equity "AAPL" "SMART")
   20 (equity "IBM" "NYSE")
   21 (equity "MSFT" "SMART")
   22 (equity "CVX" "NYSE")
   23 (equity "GE" "NYSE")
   24 (equity "T" "NYSE")
   25 (equity "PG" "NYSE")
   26 (equity "JNJ" "NYSE")
   27 (equity "PFE" "NYSE")
   28 (equity "SPY" "ARCA")
   29 (equity "SPYV" "ARCA")
   30 (equity "SPYG" "ARCA")
   31 (equity "IWM" "ARCA")
   32 (equity "IWN" "ARCA")
   33 (equity "IWO" "ARCA")
   })

(def done (atom nil))

(defn count-down [latch]
  (.countDown latch)
  latch)

(def log-agent (agent *out*))

(defn log-tick [writer received ticker-id field val]
  (let [contract (get contracts ticker-id)
        symbol (if (= :future (security-type contract))
                 (local-symbol contract)
                 (underlying-symbol contract))]
    (.write writer (str (to-long received) "," symbol "," (name field) "," val "\n"))
    writer))

(def error-agent (agent *err*))

(defn log-error [writer message]
  (.write writer (str (now) message))
  writer)

(defmulti message-handler :type)

(defmethod message-handler :error [msg]
  (let [req (if (contains? msg :request-id)
              (.toString (:request-id msg))
              "???")]
    (cond
     (contains? msg :message) (send-off error-agent log-error (str "*** [" req "] " (:message msg) "\n"))
     (contains? msg :exception) (send-off error-agent log-error (str "*** [" req "] " (.toString (:exception msg)) "\n")))
    (if (not (warning? msg))
      (do
        (await error-agent log-agent)
        #_(swap! done count-down)))))

(defmethod message-handler :price-tick [msg]
  (send-off log-agent log-tick (now) (:ticker-id msg) (:field msg) (:price msg)))

(defmethod message-handler :size-tick [msg]
  (send-off log-agent log-tick (now) (:ticker-id msg) (:field msg) (:size msg)))

(defmethod message-handler :string-tick [msg]
  (send-off log-agent log-tick (now) (:ticker-id msg) (:field msg) (:value msg)))

(defmethod message-handler :default [msg]
  (send-off error-agent log-error (str "??? unhandled message: " (prn-str msg))))

(defn not-nil? [x]
  (not (nil? x)))

(defn agent-errs []
  (filter not-nil? (map agent-errors [log-agent])))

(defn print-ticks []
  (let [connection (connect prn "localhost" 7496 2)]
    (try
      (doseq [[ticker-id contract] contracts]
        (request-market-data connection ticker-id contract))
      (Thread/sleep 60000)
      (finally
       (disconnect connection)))))

(defn record-ticks []
  (with-open [log-writer (writer (file "ticks.csv") :append false)
              error-writer (writer (file "errors.log") :append false)]
    (send-off log-agent (fn [_] log-writer))
    (send-off error-agent (fn [_] error-writer))
    (let [connection (connect message-handler "localhost" 7496 2)]
      (reset! done (java.util.concurrent.CountDownLatch. 1))
      (try
        (doseq [[ticker-id contract] contracts]
          (request-market-data connection ticker-id contract))
        (println "recording...")
        (.await @done)
        (println "done!")
        (let [errors (agent-errs)]
          (if (empty? errors)
            (Thread/sleep 250)
            (doseq [error errors]
              (println "*** Error: " error))))
        (finally
         (disconnect connection))))))
