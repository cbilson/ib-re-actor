(ns ib-re-actor.securities
  (:use [ib-re-actor.connection]
        [ib-re-actor.contracts]
        [ib-re-actor.conversions]))

(defn- message-handler [latch results error msg]
  (let [{type :type} msg]
    (condp = type

      :contract-details
      (swap! results conj (:value msg))

      :contract-details-end
      (.countDown latch)

      :error
      (if (error? msg)
        (do
          (reset! error msg)
          (.countDown latch)))

      nil)))

(defn lookup-contract
  ([attributes]
     (lookup-security attributes 1))
  ([attributes client-id]
     (let [contract (translate-to-ib-contract attributes)
           latch (java.util.concurrent.CountDownLatch. 1)
           results (atom [])
           error (atom nil)
           handler (partial message-handler latch results error)
           connection (connect handler client-id)]
       
       (try
         (request-contract-details connection 1 contract)
         (.await latch)
         (finally (disconnect connection)))

       (if (not (nil? @error))
         (let [{error-message :message exception :exception code :code} @error]
           (cond
            (not (nil? error-message)) (println "*** " error-message)
            (not (nil? exception)) (println "*** " (.toString exception))
            :default (prn @error))
           @error)
         @results))(lookup-security client-id)))

