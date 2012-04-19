(ns ib-re-actor.examples.basic.looking-up-a-security
  (:use [ib-re-actor.contract]
        [ib-re-actor.contract-details]
        [ib-re-actor.gateway]
        [ib-re-actor.util :only [to-map]]
        [clojure.pprint :only [pprint]]))

(def details (atom []))

(defn message-handler [results expected-req-id {:keys [type value request-id] :as msg}]
  (if (= expected-req-id)
    (case type
      :contract-details
      (swap! details conj value)

      :contract-details-end
      (deliver results @details)

      :error
      (if (error? msg)
        (deliver results msg))

      :next-valid-order-id nil
      :open-order-end nil
      
      (prn msg))))

(defn -main [type ticker exch ccy]
  (let [contr (doto (contract)
                (security-type (keyword type))
                (underlying-symbol ticker)
                (exchange exch)
                (currency ccy))
        result (promise)
        request-id 1]
    (with-open-connection [conn (connect (partial message-handler result request-id) 42)]
      (request-contract-details conn request-id contr)
      (if (map? @result)
        (pprint @result)
        (doseq [r @result] (-> r to-map pprint))))))