;;
;; Examples of looking up securities
;;
(ns ib-re-actor.examples.basic.looking-up-a-security.easy
  (:use [ib-re-actor.synchronous :as sync]))

;; the easy, synchronous way
(defn -main [type ticker exch]
  (-> (sync/lookup-security {:type type
                             :symbol ticker
                             :exchange exch})
      clojure.pprint/pprint))


(ns ib-re-actor.examples.basic.looking-up-a-security.low-level
  (:use [ib-re-actor.gateway]
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

(defn -main [type ticker exch]
  (let [contr {:type type
               :symbol ticker
               :exchange exch}
        result (promise)
        request-id 1]
    (with-open-connection [conn (connect (partial message-handler result request-id) 42)]
      (request-contract-details conn request-id contr)
      (if (map? @result)
        (pprint @result)
        (doseq [r @result] (-> r pprint))))))

(comment
  ;; Some other ways

  ;; if you want a specific expiry of a futures contract - you'll still
  ;; get all the expirations though
  (do-lookup {:type :future :local-symbol "ESM3" :exchange "GLOBEX"})

  ;; You could filter this down to the list you want:
  (do
    (let [result (promise)
          request-id 1]
      (with-open-connection [conn (connect (partial message-handler result request-id) 42)]
        (request-contract-details conn request-id {:type :future :local-symbol "ESM3" :exchange "GLOBEX"})
        (if (map? @result)
          (let [matches (->> @result
                             (filter #(and (= (year (-> % :summary :expiry)) 2013)
                                           (= (month (-> % :summary :expiry)) 6))))]
            (doseq [r matches] (-> r pprint))))))))


;;
;; Examples of getting historical prices
;;
(ns ib-re-actor.examples.basic.getting-historical-prices
  (:use [ib-re-actor.gateway]
        [clj-time.core :only [date-time]]))

(defn get-price []
  (let [contract {:type :future :symbol "YM"
                  :expiry (date-time 2012 6 15)
                  :exchange "ECBOT"}]
    (collect-messages :price-bar :complete
                      (fn [c r]
                        (request-historical-data c r contract
                                                 (date-time 2012 4 18 20)
                                                 1 :day 1 :day :trades true)))))

(defn get-many-prices []
  (let [contract {:type :future :symbol "YM"
                  :expiry (date-time 2012 6 15)
                  :exchange "ECBOT" :currency "USD"}]
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
  (:use [ib-re-actor.gateway]
        [clj-time.core :only [date-time]]))

(def nasdaq-future {:type :future :local-symbol "NQM2" :exchange "GLOBEX"})

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

(defn get-open-orders []
  (let [orders (promise)
        orders-accumulator (atom [])
        handler  (fn [{:keys [type] :as msg}]
                   (case type
                     :open-order (swap! orders-accumulator conj (dissoc msg :type))
                     :open-order-end (deliver orders @orders-accumulator)
                     nil))]
    (with-open-connection [c (connect handler)]
      @orders)))

(defn cancel-all-orders []
  (let [orders (get-open-orders)
        done (promise)
        handler  (fn [{:keys [type] :as msg}]
                   (prn msg))]
    (with-open-connection [c (connect handler)]
      (doseq [order orders]
        (cancel-order c (:order-id order)))
      @done)))

(defn sell-everything []
  (doseq [{:keys [position contract market-value average-cost]} (get-portfolio)] 
    (if (not= position 0)
      (let [resolved-contract (-> contract
                                  ib-re-actor.synchronous/lookup-security
                                  first
                                  :summary)]
        (println "*** closing " position "x" (:local-symbol resolved-contract))
        (let [ord {:type :market :action (if (> 0 position) :buy :sell) :quantity position}]
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

(comment
  ;; limit order for NQM2 at 2546.0
  (open-order {:type :future :local-symbol "NQM2" :exchange "GLOBEX"}
              {:action :buy :quantity 1 :type :limit :limit-price 2546.0})
  )
