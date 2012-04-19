(ns ib-re-actor.securities
  (:require [ib-re-actor.gateway :as g]
            [ib-re-actor.translation :as t]))

(defn- message-handler [target-request-id acc results {:keys [request-id type value] :as msg}]
  (if (= target-request-id request-id)
    (condp = type
      :contract-details
      (swap! acc conj value)

      :contract-details-end
      (deliver results @acc)

      :error
      (if (g/error? msg)
        (deliver results msg))

      nil)))

(defn lookup-security
  ([contract]
     (lookup-security 1 contract))
  ([client-id contract]
     (let [results (promise)
           request-id (.. (java.util.Random.) nextInt)
           handler (partial message-handler request-id (atom []) results)]
       (g/with-open-connection [connection (g/connect handler client-id)]
         (g/request-contract-details connection request-id contract)
         (if (and (map? @results) (g/error? @results))
           (do
             (println "err")
             (throw (ex-info "Failed to lookup security" @results)))
           @results)))))
