(use '[re-actor.connection]
     '[re-actor.contracts]
     '[clj-time.core :only [date-time minus]])

(defn is-end? [msg]
  (contains? [:tick-snapshot-end :error]))

(defn compute-pivots [high low close]
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

(defn warning? [msg]
  (and (contains? msg :code)
       (>= (:code msg) 2100)))

(defn report-errors [finished? msg]
  (if (= :error (:type msg))
    (do
      (cond
       (contains? msg :message) (println "*** " (:message msg))
       (contains? msg :exception) (println "*** " (.toString (:exception msg))))
      (if (not (warning? msg))
        (reset! finished? true))))
  msg)

(defn get-equity-snapshot-handler [high low close id finished? msg]
  (if (= id (:ticker-id :msg))
    (condp = (:type msg)
      
      :price-tick
      (condp = (:field msg)
       :high (do (println "High: " (:price msg))
                 (reset! high (:price msg)))
       :low (do (println "Low: " (:price msg))
                (reset! low (:price msg)))
       :close (do (println "Close: " (:price msg))
                  (reset! close (:price msg)))
       (prn "? " (:type msg) ", " (:ticker-id :msg) ": " (:price msg)))
      
      :tick-snapshot-end
      (do
        (println "finished.")
        (reset! finished? true))))
  msg)

(defn historic-price-handler [high low close id finished? msg]
  (if (= id (:request-id msg))
    (condp = (:type msg)

      :price-bar
      (do
        (reset! high (:high msg))
        (reset! low (:low msg))
        (reset! close (:close msg))
        (println "bar received. high: " @high ", low: " @low ", close: " @close))
      
      :complete
      (do
        (println "finished with historic request " id)
        (reset! finished? true))))
  msg)

(defn print-message [msg]
  (prn msg)
  msg)

(defn compute-daily-pivots [contract date]
  (let [high (atom 0.0)
        low (atom 0.0)
        close (atom 0.0)
        finished? (atom false)
        handler (comp (partial historic-price-handler high low close 1 finished?)
                      (partial report-errors finished?)
                      print-message)
        connection (connect handler)]
    (try 
      (request-historical-data connection 1 contract date 1 :day 1 :day :midpoint)
      (while (not @finished?)
        (println "waiting...")
        (java.lang.Thread/sleep 250))
      (compute-pivots @high @low @close)
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
  (let [contract (futures-contract "YM" "ECBOT" (date-time 2011 12))
        pivots (compute-daily-pivots contract date)
        
        close (:close pivots)]
    ))
