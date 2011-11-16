(ns ib-re-actor.examples.security-lookup
  (:use [ib-re-actor.connection]
        [ib-re-actor.contracts]
        [clj-time.core :only [date-time minus now]]))

(defn message-handler [latch results msg]
  (let [{type :type} msg]
    (condp = type
      
      :contract-details
      (swap! results conj (:value msg))
      
      :contract-details-end
      (.countDown latch)
      
      :error
      (let [{error-message :message exception :exception code :code} msg]
        (cond
         (not (nil? error-message)) (println "*** " error-message)
         (not (nil? exception)) (println "*** " (.toString exception)))
        (if (not (warning? msg))
          (.countDown latch)
          (prn msg)))

      nil)))

(defn lookup-security [contract]
  (let [latch (java.util.concurrent.CountDownLatch. 1)
        results (atom [])
        handler (partial message-handler latch results)
        connection (connect handler)]
    (try
      (request-contract-details connection 1 contract)
      (.await latch)
      @results
      (finally (disconnect connection)))))

