(ns ib-re-actor.synchronous
  "Functions for doing things with interactive brokers in a synchronous manner.
   Mostly these are just wrappers that create a temporary handler function,
   wait for a response, then unsubscribe. These are much easier to use in an
   interactive context (such as when using the REPL) but probably not what you
   would want to use in an application, as the asynchronous API is a much more
   natural fit for building programs that react to events in market data."
  (:require [ib-re-actor.gateway :refer [with-subscription end? error-end? request-end?] :as g]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]))

(defn get-time
  "Returns the server time"
  []
  (let [result (promise)
        handler (fn [{:keys [type value] :as msg}]
                  (cond
                   (and (= type :current-time))
                   (deliver result value)

                   (error-end? msg)
                   (deliver result msg)))]
    (with-subscription handler
      (g/request-current-time)
      @result)))

(defn get-contract-details
  "Gets details for the specified contract."
  [con]
  (let [responses (atom nil)
        req-id (g/get-request-id)
        results (promise)
        handler (fn [{:keys [type request-id value] :as msg}]
                  (cond
                   (and (= type :contract-details) (= req-id request-id))
                   (swap! responses conj value)

                   (request-end? req-id msg)
                   (deliver results @responses)

                   (error-end? req-id msg)
                   (deliver results msg)))]
    (with-subscription handler
      (g/request-contract-details req-id con)
      @results)))

(defn get-current-price
  "Gets the current price for the specified contract."
  [con]
  (let [fields (atom nil)
        req-ticker-id (g/get-request-id)
        result (promise)
        handler (fn [{:keys [type ticker-id request-id field value code] :as msg}]
                  (cond
                   (and (= type :tick) (= ticker-id req-ticker-id))
                   (swap! fields assoc field value)

                   (request-end? req-ticker-id msg)
                   (deliver result @fields)

                   (error-end? req-ticker-id msg)
                   (deliver result msg)))]
    (with-subscription handler
      (g/request-market-data con req-ticker-id "" true)
      @result)))

(defn execute-order
  "Executes an order, returning only when the order is filled or pending."
  [contract order]
  (let [ord-id (g/get-order-id)
        market-closed? (atom false)
        updates (atom [])
        result (promise)
        handler (fn [{:keys [type request-id order-id status code message] :as msg}]
                  (let [done (#{:filled :cancelled :inactive} status)
                        this-order (= ord-id (or order-id request-id))]
                    (cond
                     (and this-order done)
                     (deliver result (conj @updates msg))

                     (and this-order @market-closed? (= :pre-submitted status))
                     (deliver result (conj @updates msg))

                     (and this-order (= :error type) (= 399 code))
                     (do
                       (reset! market-closed? true)
                       (swap! updates conj msg))

                     (end? ord-id msg)
                     (deliver result (conj @updates msg))

                     this-order
                     (swap! updates conj msg))))]
    (with-subscription handler
      (g/place-order ord-id contract order)
      @result)))

(defn get-historical-data
  "Gets historical price bars for a contract."
  [contract end-time duration duration-unit bar-size bar-size-unit
   what-to-show use-regular-trading-hours?]
  (let [req-id (g/get-request-id)
        accum (atom [])
        results (promise)
        handler (fn [{:keys [type request-id] :as msg}]
                  (cond
                   (end? req-id msg)
                   (deliver results (conj @accum msg))

                   (and (= req-id request-id) (= type :price-bar))
                   (swap! accum conj msg)))]
    (with-subscription handler
      (g/request-historical-data req-id contract end-time duration duration-unit
                                 bar-size bar-size-unit what-to-show use-regular-trading-hours?)
      @results)))

(defn get-open-orders
  "Gets all open orders for the current connection."
  []
  (let [accum (atom [])
        results (promise)
        handler (fn [{:keys [type] :as msg}]
                  (cond
                   (= :open-order type)
                   (swap! accum conj msg)

                   (error-end? msg)
                   (deliver results (conj @accum msg))

                   (end? msg)
                   (deliver results @accum)))]
    (with-subscription handler
      (g/request-open-orders)
      @results)))

(defn cancel-order [order-id]
  (g/cancel-order order-id)
  nil)
