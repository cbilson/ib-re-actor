(ns ib-re-actor.examples.gap-fade
  (:use [ib-re-actor.connection]
        [ib-re-actor.contracts]
        [clj-time.core :only [date-time minus]]))

(defn compute-pivots [{high :high low :low close :close}]
  (let [pp (/ (+ high low close) 3)
        r1 (- (* 2 pp) low)
        s1 (- (* 2 pp) high)
        r2 (+ pp (- r1 s1))
        s2 (- pp (- r1 s1))
        r3 (+ high (* 2 (- pp low)))
        s3 (- low (* 2 (- high pp)))]
    {:high high :low low :close close
     :r3 r3 :r2 r2 :r1 r1
     :pp pp
     :s1 s1 :s2 s2 :s3 s3}))

(def historic-prices (atom {}))

(defmulti historic-handler :type)
(defmethod historic-handler :error [msg]
  (cond
   (contains? msg :message) (println "*** " (:message msg))
   (contains? msg :exception) (println "*** " (.toString (:exception msg))))
  (if (not (warning? msg))
    (swap! historic-prices assoc :done? true :failed? true)))

(defmethod historic-handler :price-bar [msg]
  (swap! historic-prices assoc
         :bars (conj (:bars @historic-prices)
                     {:time (:time msg)
                      :high (:high msg)
                      :low (:low msg)
                      :close (:close msg)})))

(defmethod historic-handler :complete [msg]
  (swap! historic-prices assoc :done? true))

(defmethod historic-handler :default [msg]
  (prn msg))

(defn compute-daily-pivots [contract date]
  (let [connection (connect historic-handler)]
    (try
      (reset! historic-prices {})
      (request-historical-data connection 1 contract date 1 :day 1 :day :trades)
      (while (not (:done? @historic-prices))
        (print ".")
        (java.lang.Thread/sleep 250))
      (if (:failed? @historic-prices)
        (println "*** failed to get historic prices")
        (compute-pivots (first (:bars @historic-prices))))
      (finally
       (disconnect connection)))))

(defmulti tick-handler :type)

(defmethod tick-handler :error [msg]
  (cond
   (contains? msg :message) (println "*** " (:message msg))
   (contains? msg :exception) (println "*** " (.toString (:exception msg))))
  (if (not (warning? msg))
    (swap! historic-prices assoc :done? true :failed? true)))

(def current-prices (atom {}))
(defmethod tick-handler :price-tick [{ticker-id :ticker-id field :field price :price}]
  (swap! current-prices assoc ticker-id {}))
(defmethod tick-handler :price-tick :bid-size [{price :price}])
(defmethod tick-handler :price-tick :ask-price [{price :price}])
(defmethod tick-handler :price-tick :ask-size [{price :price}])


(def last-ticker-id (atom 0))
(defn watch
  "Watch a contract and keep a scoreboard updates with the latest ticks"
  [contract]
  (let [connection (connect tick-handler)
        ticker-id (swap! last-ticker-id inc)]
    (request-market-data connection ticker-id contract ticker-id)))

(defn print-pivots [pivots]
  (println "H/L/C: " (:high pivots) "/" (:low pivots) "/" (:close pivots))
  (println "r3: " (:r3 pivots))
  (println "r2: " (:r2 pivots))
  (println "r1: " (:r1 pivots))
  (println "pp: " (:pp pivots))
  (println "s1: " (:s1 pivots))
  (println "s2: " (:s2 pivots))
  (println "s3: " (:s3 pivots)))

(defn get-opening-trade [date]
  (let [cash-contract (index "INDU" "NYSE")
        future (futures-contract "YM" "ECBOT" (date-time 2011 12))
        pivots (compute-daily-pivots cash-contract date)]
    (watch )))

