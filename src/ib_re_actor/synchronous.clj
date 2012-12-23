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

(defn- should-accept?
  "If a message is one of the ones we want for this request, an end of request message, or
an error of some kind, we need to add it to the accumulator."
  [req-id accept? msg]
  (or (if (fn? accept?) (accept? msg) true)
      (g/is-end-for? req-id msg)))

(defn- is-done?
  "As soon as we've gotten whatever data needed to satisfy the request, an end of request
message, or an error of some kind, we can stop waiting."
  [req-id done? msgs]
  (let [last-msg (first msgs)]
    (or (g/is-end-for? req-id last-msg)
        (and (fn? done?) (done? msgs)))))

(defn- accumulate-responses
  "Given some functions, subscribes to the message stream, adding each message to
   an accumulator, then cleans up when done."
  [req-id accept? done? completion-fn subscribe-fn & args]
  (let [responses (atom nil)
        result (promise)
        handler (fn [msg]
                  (when (should-accept? req-id accept? msg)
                    (swap! responses conj msg)))
        response-watch (fn [_ k _ v]
                         (when (is-done? req-id done? v)
                           (remove-watch responses k)
                           (deliver result v)))]
    (if (not (g/connected?))
      "Not connected."
      (do
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
            (log/debug "completed")))))))

(defn get-time
  "Returns the server time"
  []
  (accumulate-responses nil #(= :current-time (:type %))
                    (comp not empty?) nil
                    g/request-current-time))

(defn get-contract-details
  "Gets details for the specified contract."
  [contract]
  (let [req-id (g/get-request-id)]
    (accumulate-responses req-id #(= req-id (:request-id %))
                      nil nil
                      g/request-contract-details
                      req-id contract)))

(defn get-current-price
  "Gets the current price for the specified contract."
  [con]
  (let [trading-fields [:open-tick :bid-price :close-price :last-size :low
                        :ask-size :bid-size :last-price :ask-price :high :volume]
        closed-fields [:close-price :bid-price :bid-size :ask-price :ask-size]
        looks-closed? (fn [accum] (= (:bid-price accum) -1.0))
        accept? (fn [{:keys [type contract]}]
                  (and (= type :tick) (= contract con)))
        done? (fn [accum]
                (let [received-fields (-> accum keys set)]
                  (every? received-fields
                          (if (looks-closed? accum) closed-fields trading-fields))))
        complete (partial g/cancel-market-data con)]
    (accumulate-responses nil accept? done? complete
                      g/request-market-data con)))

(defn execute-order
  "Executes an order, returning only when the order is filled."
  [contract order]
  (let [order-id (g/get-order-id)
        accept? #(= order-id (:order-id %))
        done? (partial some (fn [{:keys [type status]}]
                              (and (= :order-status type)
                                   (= :filled status))))]
    (log/debug "Order-Id: " order-id)
    (accumulate-responses nil accept? done? nil g/place-order
                      order-id contract order)))

(defn get-historical-data [contract end-time duration duration-unit bar-size
                           bar-size-unit what-to-show
                           use-regular-trading-hours?]
  "Gets historical price bars for a contract."
  (let [request-id (g/get-request-id)
        accept? #(= request-id (:request-id %))
        done? (partial some #(= :price-bar-complete (:type %)))
        complete (fn [])
        responses (accumulate-responses nil accept? done? complete
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
        done? (partial some #(= :open-order-end (:type %)))
        complete (fn [])
        responses (accumulate-responses nil accept? done? complete
                                    g/request-open-orders)
        errors (filter #(= :error (:type %)) responses)]
    (if (empty? errors)
      (->> responses
           (filter #(= :open-order (:type %)))
           (map #(select-keys % [:order-id :contract :order
                                 :order-state])))
      errors)))
