(ns ib-re-actor.securities
  (:require [ib-re-actor.gateway :as g]
            [ib-re-actor.translation :as t]))

(defn- message-handler [latch results error msg]
  (let [{type :type} msg]
    (condp = type

      :contract-details
      (swap! results conj (:value msg))

      :contract-details-end
      (.countDown latch)

      :error
      (if (g/error? msg)
        (do
          (reset! error msg)
          (.countDown latch)))

      nil)))

(defn lookup-security
  [client-id contract]
  (let [latch (java.util.concurrent.CountDownLatch. 1)
        results (atom [])
        error (atom nil)
        handler (partial message-handler latch results error)]
    (g/with-open-connection [connection (g/connect handler client-id)]
      (g/request-contract-details connection 1 contract)
      (.await latch))
    (if (not (nil? @error))
      (let [{error-message :message exception :exception code :code} @error]
        (cond
         (not (nil? error-message)) (println "*** " error-message)
         (not (nil? exception)) (println "*** " (.toString exception))
         :default (prn @error))
        @error)
      @results)))

