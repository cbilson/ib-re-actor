;;
;; Examples of looking up securities
;;
(ns ib-re-actor.examples.basic.looking-up-a-security.easy
  (:use [ib-re-actor.contract]
        [ib-re-actor.util :only [to-map]]
        [ib-re-actor.securities :only [lookup-security]]
        [clojure.pprint :only [pprint]]))

;; the easy, synchronous way
(defn -main [type ticker exch ccy]
  (doseq [c (->> (doto (contract)
                   (security-type (keyword type))
                   (underlying-symbol ticker)
                   (exchange exch)
                   (currency ccy))
                 lookup-security
                 (map to-map)
                 )]
    (pprint c)))

(ns ib-re-actor.examples.basic.looking-up-a-security.low-level
  (:use [ib-re-actor.contract]
        [ib-re-actor.contract-details]
        [ib-re-actor.gateway]
        [ib-re-actor.util :only [to-map]]
        [clojure.pprint :only [pprint]]
        [clj-time.core :only [year month]]))

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

(defn do-lookup [contr]
  )

(defn print-result []
  )

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

(comment
  ;; Some other ways

  ;; if you want a specific expiry of a futures contract - you'll still
  ;; get all the expirations though
  (do-lookup (doto (futures-contract)
               (local-symbol "ESM3")
               (exchange "GLOBEX")))

  ;; You could filter this down to the list you want:
  (do
    (let [result (promise)
          request-id 1]
      (with-open-connection [conn (connect (partial message-handler result request-id) 42)]
        (request-contract-details conn request-id (doto (futures-contract) (local-symbol "ESM3") (exchange "GLOBEX")))
        (if (map? @result)
          (let [matches (->> @result
                             (filter #(and (= (year (-> % summary expiry)) 2013)
                                           (= (month (-> % summary expiry)) 6))))]
            (doseq [r matches] (-> r to-map pprint)))))))
  )


;;
;; Examples of getting historical prices
;;
(ns ib-re-actor.examples.basic.getting-historical-prices
  (:use [ib-re-actor.contract]
        [ib-re-actor.gateway]
        [ib-re-actor.util :only [to-map]]
        [clojure.pprint :only [pprint]]
        [clj-time.core :only [year month]]))

()