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

(defmacro subscribe-for
  "Given a function to call and a predicate, subscribes to the current connection
   and returns the first message that matches the predicate, unsubscribing once
   a message has been received."
  [fn pred]
  `(let [res# (promise)
         handler# (fn [msg#]
                    (log/info "Received: " msg#)
                    (cond
                     (~pred msg#) (deliver res# msg#)
                     (g/is-end? msg#) (deliver res# msg#)))]
     (g/subscribe handler#)
     (try
       (~fn)
       (log/info "waiting for response")
       @res#
       (finally
         (log/info "unsubscribing")
         (g/unsubscribe handler#)))))

(defmacro get-response
  [request-fn & args]
  `(let [res# (promise)
         req-id# (g/get-request-id)
         handler# (fn [{msg-req-id# :request-id :as msg#}]
                    (log/info "Received: " msg#)
                    (cond
                     (= req-id# msg-req-id#) (deliver res# msg#)
                     (g/is-end? msg#) (deliver res# msg#)))]
     (g/subscribe handler#)
     (try
       (-> req-id# (~request-fn ~@args))
       (log/info "waiting for response")
       @res#
       (finally (g/unsubscribe handler#)))))

(defmacro get-responses [request-fn & args]
  `(let [res# (future)
         accum# (atom nil)
         req-id# (g/get-request-id)
         handler# (fn [{msg-req-id# :request-id type# :type :as msg#}]
                    (log/info "Received: " msg#)
                    (when (= msg-req-id# req-id#)
                      (swap! accum# conj msg#)
                      (when (g/is-end? msg#)
                        (deliver res# @accum#))))]
     (g/subscribe handler#)
     (try
       (-> req-id# (~request-fn ~@args))
       (log/info "waiting for response")
       @res#
       (finally (g/unsubscribe handler#)))))

(defn get-time []
  (subscribe-for g/request-current-time
                 #(= :current-time (:type %))))

(defn get-contract-details [contract]
  (get-response g/request-contract-details contract))

(defn reduce-responses [accept? reduce-fn done? completion-fn subscribe-fn & args]
  "Given some functions, subscribes to the message stream, reducing each message into
   an accumulator, then cleans up when done."
  (let [responses (atom nil)
        result (promise)
        handler (fn [msg]
                  (when (accept? msg)
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
        (log/info "unsubscribing")
        (g/unsubscribe handler)
        (log/info "completing")
        (completion-fn)
        (log/info "completed")))))

(defn get-current-price [con]
  (let [fields [:open-tick :bid-price :close-price :last-size :low :ask-size :bid-size :last-price :ask-price :high :volume]
        accept? (fn [{:keys [type contract]}]
                  (and (= type :tick) (= contract con)))
        reduce-fn (fn [accum {:keys [field value]}]
                    (assoc accum field value))
        done? (fn [accum]
                (let [received-fields (-> accum keys set)]
                  (every? received-fields fields)))
        complete (partial g/cancel-market-data con)]
    (reduce-responses accept? reduce-fn done? complete g/request-market-data con)))

(defn execute-order [contract order]
  (let [order-id (g/get-order-id)
        accept? #(= order-id (:order-id %))
        reduce-fn conj
        done? #(some (fn [{:keys [type status]}]
                       (and (= :order-status type) (= :filled status))))
        complete (fn [])]
    (reduce-responses accept? reduce-fn done? complete g/place-order contract order)))

(defn get-historic-prices [contract end-time duration duration-unit bar-size
                           bar-size-unit what-to-show use-regular-trading-hours?]
  (let [request-id (g/get-request-id)
        accept? #(= request-id (:request-id %))
        reduce-fn conj
        done? (partial some #(= :price-bar-complete (:type %)))
        complete (fn [])
        responses (reduce-responses accept? reduce-fn done? complete
                                    g/request-open-orders request-id
                                    contract end-time duration-unit
                                    bar-size bar-size-unit what-to-show
                                    use-regular-trading-hours?)
        errors (filter #(= :error (:type %)) responses)]
    (if errors
      errors
      (->> responses
           (filter #(= :price-bar (:type %)))
           (map #(select-keys % [:time :open :high :low :close
                                 :trade-count :volume :has-gaps?]))))))

(defn get-open-orders []
  (let [accept? #(#{:open-order :open-order-end :error} (:type %))
        reduce-fn conj
        done? (partial some #(= :open-order-end (:type %)))
        complete (fn [])
        responses (reduce-responses accept? reduce-fn done? complete
                                    g/request-open-orders)
        errors (filter #(= :error (:type %)) responses)]
    (if errors
      errors
      (->> responses
           (filter #(= :open-order (:type %)))
           (map #(select-keys % [:order-id :contract :order
                                 :order-state]))))))
