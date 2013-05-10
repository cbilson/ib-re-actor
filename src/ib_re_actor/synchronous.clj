(ns ib-re-actor.synchronous
  "Functions for doing things with interactive brokers in a synchronous manner.
   Mostly these are just wrappers that create a temporary handler function,
   wait for a response, then unsubscribe. These are much easier to use in an
   interactive context (such as when using the REPL) but probably not what you
   would want to use in an application, as the asynchronous API is a much more
   natural fit for building programs that react to events in market data."
  (:require [ib-re-actor.gateway :as g]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]))

(defn- reduce-responses [accept? reduce-fn done? completion-fn subscribe-fn & args]
  "Given some functions, subscribes to the message stream, reducing each message into
   an accumulator, then cleans up when done."
  (let [responses (atom nil)
        result (promise)
        handler (fn [msg]
                  (when (if (fn? accept?)
                          (accept? msg)
                          true)
                    (if (g/is-end? msg)
                      (deliver result msg)
                      (swap! responses reduce-fn msg))))
        response-watch (fn [_ k _ v]
                         (when (done? v)
                           (remove-watch responses k)
                           (deliver result v)))]
    (add-watch responses :result-watch response-watch)
    (g/subscribe handler)
    (try
      (apply subscribe-fn args)
      @result
      (finally
        (log/debug "unsubscribing")
        (g/unsubscribe handler)
        (log/debug "completing")
        (if (fn? completion-fn)
          (completion-fn))
        (log/debug "completed")))))

(defn get-time
  "Returns the server time"
  []
  (reduce-responses #(= :current-time (:type %))
                    conj (comp not empty?) nil
                    g/request-current-time))

(defn get-contract-details
  "Gets details for the specified contract."
  [contract]
  (let [req-id (g/get-request-id)]
    (reduce-responses #(= req-id (:request-id %))
                      conj (comp not empty?) nil
                      g/request-contract-details
                      req-id contract)))

(defn get-current-price
  "Gets the current price for the specified contract."
  [con]
  (let [fields (atom nil)
        req-ticker-id (swap! g/last-ticker-id inc)
        result (promise)
        handler (fn [{:keys [type ticker-id request-id field value code] :as msg}]
                  (cond
                   (and (= type :tick) (= ticker-id req-ticker-id))
                   (swap! fields assoc field value)

                   (and (= type :tick-snapshot-end) (= req-ticker-id request-id))
                   (deliver result @fields)

                   (and (= type :error) (= 504 code))
                   (deliver result msg)))]
    (g/subscribe handler)
    (try
      (g/request-market-data con req-ticker-id "" true)
      @result
      (finally
        (log/debug "unsubscribing")
        (g/unsubscribe handler)
        (log/debug "completing")))))

(defn execute-order
  "Executes an order, returning only when the order is filled."
  [contract order]
  (let [order-id (g/get-order-id)
        accept? #(= order-id (:order-id %))
        reduce-fn (fn [coll x] (log/debug x) (conj coll x))
        done? (partial some (fn [{:keys [type status]}]
                              (and (= :order-status type)
                                   (= :filled status))))]
    (log/debug "Order-Id: " order-id)
    (reduce-responses accept? reduce-fn done? nil g/place-order
                      order-id contract order)))

(defn get-historical-data [contract end-time duration duration-unit bar-size
                           bar-size-unit what-to-show
                           use-regular-trading-hours?]
  "Gets historical price bars for a contract."
  (let [request-id (g/get-request-id)
        accept? #(= request-id (:request-id %))
        reduce-fn conj
        done? (partial some #(= :price-bar-complete (:type %)))
        complete (fn [])
        responses (reduce-responses accept? reduce-fn done? complete
                                    g/request-historical-data request-id
                                    contract end-time duration duration-unit
                                    bar-size bar-size-unit what-to-show
                                    use-regular-trading-hours?)
        errors (filter #(= :error (:type %)) responses)]
    (if (not (empty? errors))
      errors
      (->> responses
           (filter #(= :price-bar (:type %)))
           (map #(select-keys % [:time :open :high :low :close
                                 :trade-count :volume :has-gaps?]))))))

(defn get-open-orders
  "Gets all open orders for the current connection."
  []
  (let [accept? #(#{:open-order :open-order-end :error} (:type %))
        reduce-fn conj
        done? (partial some #(= :open-order-end (:type %)))
        complete (fn [])
        responses (reduce-responses accept? reduce-fn done? complete
                                    g/request-open-orders)
        errors (filter #(= :error (:type %)) responses)]
    (if (empty? errors)
      (->> responses
           (filter #(= :open-order (:type %)))
           (map #(select-keys % [:order-id :contract :order
                                 :order-state])))
      errors)))
