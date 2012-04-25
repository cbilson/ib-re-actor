;;
;; Examples of looking up securities
;;
(ns ib-re-actor.examples.basic.looking-up-a-security.easy
  (:use [ib-re-actor.contract]
        [ib-re-actor.contract-details]
        [ib-re-actor.synchronous :as sync]))

;; the easy, synchronous way
(defn -main [type ticker exch ccy]
  (doseq [c (->> (doto (contract)
                   (security-type (keyword type))
                   (underlying-symbol ticker)
                   (exchange exch)
                   (currency ccy))
                 sync/lookup-security
                 (map ib-re-actor.util/to-map)
                 )]
    (clojure.pprint/pprint c)))

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
        [clj-time.core :only [date-time]]))

(defn get-price []
  (let [contract (doto (futures-contract)
                   (underlying-symbol "YM")
                   (expiry (date-time 2012 6 15))
                   (exchange "ECBOT")
                   (currency "USD"))]
    (collect-messages :price-bar :complete
                      (fn [c r]
                        (request-historical-data c r contract
                                                 (date-time 2012 4 18 20)
                                                 1 :day 1 :day :trades true)))))

(defn get-many-prices []
  (let [contract (doto (futures-contract)
                   (underlying-symbol "YM")
                   (expiry (date-time 2012 6 15))
                   (exchange "ECBOT")
                   (currency "USD"))]
    (collect-messages :price-bar :complete
                      (fn [c r]
                        (request-historical-data c r contract
                                                 (date-time 2012 4 18 20)
                                                 30 :days 1 :day :trades true)))))

;;
;; Examples of managing orders orders
;;
(ns ib-re-actor.examples.basic.orders
  (:require [ib-re-actor.synchronous :as sync])
  (:use [ib-re-actor.contract]
        [ib-re-actor.order]
        [ib-re-actor.gateway]
        [clj-time.core :only [date-time]]))

(def nasdaq-future (doto (futures-contract "NQM2" "GLOBEX")))

(defn get-current-price []
  (sync/get-current-price nasdaq-future))

(defn get-account-update []
  (let [results (promise)
        attributes (atom {})
        handler  (fn [{:keys [type key value currency] :as msg}]
                   (case type
                     :update-account-value (swap! attributes assoc key {:value value :currency currency})
                     :update-account-time (deliver results @attributes)
                     :error (if (error? msg) (deliver results msg))
                     nil))]
    (with-open-connection [c (connect handler)]
      (request-account-updates c true nil)
      @results)))

(defn get-portfolio []
  (sync/get-portfolio))

(defn cancel-all-orders [connection]
  (let [done-with-orders (promise)
        orders (atom [])
        handler  (fn [{:keys [type] :as msg}]
                   (prn msg)
                   (case type
                     :open-order-end (deliver done-with-orders true)
                     nil))]
    (with-open-connection [c (connect handler)]
      @done-with-orders)))

(defn sell-everything []
  (doseq [{:keys [position contract market-value average-cost]} (get-portfolio)] 
    (if (not= position 0)
      (let [resolved-contract (-> contract
                                  ib-re-actor.synchronous/lookup-security
                                  first
                                  ib-re-actor.contract-details/summary)]
        (println "*** closing " position "x" (local-symbol resolved-contract))
        (let [ord (order :market (if (> 0 position) :buy :sell) position)]
          (sync/execute-order resolved-contract ord))))))

(defn open-order [contract order]
  (let [result (promise)
        handler (fn [{:keys [type field value status] :as msg}]
                  (case type
                    :open-order (swap! result msg)
                    :error (if (error? msg)
                             (deliver result false))
                    nil))]
    (with-open-connection [conn (connect handler)]
      (place-order conn contract order)
      @result)))

