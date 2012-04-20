(ns ib-re-actor.synchronous
  (:require [ib-re-actor.gateway :as g]
            [ib-re-actor.translation :as t]))

(defn lookup-security
  "Synchronously lookup a security and return the matches."
  ([contract]
     (lookup-security 1 contract))
  ([client-id contract]
     (let [results (promise)
           this-request-id (.. (java.util.Random.) nextInt)
           acc (atom [])
           handler (fn [{:keys [request-id type value] :as msg}]
                     (if (= this-request-id request-id)
                       (condp = type
                         :contract-details
                         (swap! acc conj value)
                         
                         :contract-details-end
                         (deliver results @acc)

                         :error
                         (if (g/error? msg)
                           (deliver results msg))
                         
                         nil)))]
       (g/with-open-connection [connection (g/connect handler client-id)]
         (g/request-contract-details connection this-request-id contract)
         (if (and (map? @results) (g/error? @results))
           (throw (ex-info "Failed to lookup security" @results))
           @results)))))