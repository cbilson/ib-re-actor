(ns ib-re-actor.synchronous
  (:require [ib-re-actor.gateway :as g]
            [ib-re-actor.translation :as t]
            [ib-re-actor.order :as o]))

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

(defn execute-order [contract ord-typ act qty outside-rth?]
  "Place a market order, wait for it to finish and report on progress."
  (let [order (doto (o/order ord-typ act qty)
                (o/outside-regular-trading-hours? outside-rth?)
                (o/transmit? true))
        order-id (promise)
        result (promise)
        report (atom {:progress []})
        add-progress (fn [{progress :progress :as report} msg]
                       (assoc report :progress (conj progress msg)))
        handler (fn [{:keys [type field value status] :as msg}]
                  (case type
                    :next-valid-order-id (deliver order-id value)
                    :open-order (swap! report add-progress msg)
                    :execution-details (swap! report add-progress msg)
                    :order-status
                    (do
                      (swap! report add-progress msg)
                      (if (= :filled status)
                        (deliver result @report)))
                    
                    :error (if (g/error? msg)
                             (deliver result false))
                    nil))]
    (g/with-open-connection [conn (g/connect handler)]
      (g/place-order conn @order-id contract order)
      @result)))

(defn get-current-price [contract]
  (let [results (promise)
        snapshot (atom {})
        id (.. (java.util.Random.) nextInt)
        handler  (fn [{:keys [type field value] :as msg}]
                   (case type
                     :tick (swap! snapshot assoc field value)
                     :tick-snapshot-end (deliver results @snapshot)
                     :error (if (g/error? msg) (deliver results msg))
                     nil))]
    (g/with-open-connection [c (g/connect handler)]
      (g/request-market-data c id contract [] true)
      @results)))
