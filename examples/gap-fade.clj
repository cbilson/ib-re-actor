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

(defn is-end? [msg]
  (contains? [:tick-snapshot-end :error]))

(defn warning? [msg]
  (and (contains? msg :code)
       (>= (:code msg) 2100)))

(defn process-error [msg]
  (cond
   (contains? msg :message) (println "*** " (:message msg))
   (contains? msg :exception) (println "*** " (.toString (:exception msg))))
  (if (not (warning? msg))
    (swap! historic-prices assoc :done? true :failed? true)))(defmulti historic-handler :type)

(def historic-prices (atom {}))
(defmethod historic-handler :error [msg] (process-error msg))
(defmethod historic-handler :price-bar [msg]
  (swap! historic-prices assoc :bars (conj (:bars @historic-prices)) {:high (:high msg)
                               :low (:low msg)
                               :close (:close msg)}))

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
        nil
        (compute-pivots @historic-prices))
      (finally
       (disconnect connection)))))

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
        future (futures-contract "YM" "ECBOT" (date-time 2011 12))]
    (if-let [pivots (compute-daily-pivots cash-contract date)]
      (print-pivots pivots))))

(def news-complete? (atom false))
(defmulti news-handler :type)
(defmethod news-handler :error [msg]
  (process-error msg)
  (if (not (warning? msg))
    (reset! news-complete? true)))
(defmethod news-handler :default [msg] (prn msg))
(defmethod news-handler :update-news-bulletin [{message :message}]
  (println message))

(defn watch-news []
  (let [connection (connect news-handler)]
    (try
      (reset! news-complete? false)
      (request-news-bulletins connection)
      (while (not @news-complete?)
        (java.lang.Thread/sleep 100))
      (finally (disconnect connection)))))

(def fundamental-data-complete? (atom false))
(defmulti fundamental-data-handler :type)
(defmethod fundamental-data-handler :error [msg]
  (process-error msg)
  (if (not (warning? msg))
    (reset! fundamental-data-complete? true)))
(defmethod fundamental-data-handler :default [msg] (prn msg))
(defmethod fundamental-data-handler :fundamental-data [{report :report}]
  (prn report)
  (reset! fundamental-data-complete? true))

(defn get-fundamentals []
  (let [connection (connect fundamental-data-handler)]
    (try
      (reset! fundamental-data-complete? false)
      (request-fundamental-data connection -1 (equity "C" "NYSE") :summary)
      (while (not @fundamental-data-complete?)
        (java.lang.Thread/sleep 100))
      (finally (disconnect connection)))))
