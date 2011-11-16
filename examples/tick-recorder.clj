(ns ib-re-actor.examples.tick-recorder
  (:use [ib-re-actor.connection]
        [ib-re-actor.contracts]
        [clj-time.core :only [date-time minus now]]
        [clj-time.coerce :only [to-long]]
        [clojure.java.io]))

(def contracts {1 (futures-contract "YMZ1" "ECBOT") ; ?
                2 (futures-contract "YMH2" "ECBOT") ; ?
                3 (index "YM" "ECBOT")
                4 (futures-contract "TFZ1" "NYBOT")
                5 (futures-contract "TFH2" "NYBOT")
                6 (index "TF" "NYBOT")
                7 (futures-contract "ESZ1" "GLOBEX")
                8 (futures-contract "ESH2" "GLOBEX")
                9 (index "ES" "GLOBEX")
                10 (futures-contract "NQZ1" "GLOBEX")
                11 (futures-contract "NQH2" "GLOBEX")
                12 (index "NQ" "GLOBEX")
                13 (futures-contract "E7Z1" "GLOBEX")
                14 (futures-contract "EAZ1" "GLOBEX") ; ?
                15 (futures-contract "ZQZ1" "ECBOT")  ; ?
                16 (futures-contract "ZNZ1" "ECBOT")  ; ?
                17 (index "TICK-NYSE" "NYSE")
                18 (index "TRIN-NYSE" "NYSE")})

(def handler-done (atom false))

(def log-agent (agent nil))

(defn log-tick [writer received ticker-id field val]
  (let [contract (get contracts ticker-id)
        symbol (if (= :future (security-type contract))
                 (local-symbol contract)
                 (underlying-symbol contract))]
    (.write writer (str (to-long received) "," symbol "," (name field) "," val "\n"))
    writer))

(def error-agent (agent nil))

(defn log-error [writer message]
  (.write writer message)
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
        (await error-agent)
        (reset! handler-done true)))))

(defmethod message-handler :price-tick [msg]
  (send-off log-agent log-tick (now) (:ticker-id msg) (:field msg) (:price msg)))

(defmethod message-handler :size-tick [msg]
  (send-off log-agent log-tick (now) (:ticker-id msg) (:field msg) (:size msg)))

(defmethod message-handler :string-tick [msg]
  (send-off log-agent log-tick (now) (:ticker-id msg) (:field msg) (:value msg)))

(defmethod message-handler :default [msg]
  (prn msg))

(defn not-nil? [x]
  (not (nil? x)))

(defn agent-errs []
  (filter not-nil? (map agent-errors [log-agent])))

(defn record-ticks []
  (with-open [log-writer (writer (file "ticks.csv") :append false)
              error-writer (writer (file "errors.log") :append false)]
    (send-off log-agent (fn [_] log-writer))
    (send-off error-agent (fn [_] error-writer))
    (let [connection (connect message-handler)] 
      (try
        (doseq [[ticker-id contract] contracts]
          (request-market-data connection ticker-id contract))
        (while (not (:done @handler-done))
          (let [errors (agent-errs)]
            (if (empty? errors)
              (Thread/sleep 250)
              (doseq [error errors]
                (println "*** Error: " error))))
          (reset! handler-done true))
        (finally
         (disconnect connection))))))
