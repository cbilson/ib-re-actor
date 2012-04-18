(ns ib-re-actor.execution-filter
  (:require [ib-re-actor.translation :as t]))

(defprotocol ExecutionFilter
  (client-id [this] [this val]
    "Filter the results of request-executions based on the clientId.")
  (account-code [this] [this val]
    "Filter the results of request-executions based on an account code.
     Note: this is only relevant for Financial Advisor (FA) accounts.")
  (after-time [this] [this val]
    "Filter the results of request-executions based on execution
     reports received after the specified time.")
  (order-symbol [this] [this val]
    "Filter the results of request-executions based on the order symbol.")
  (security-type [this] [this val]
    "Filter the results of request-executions based on the order security type.
     Value values: :equity, :option, :future, :index, :future-option, :cash, :bag")
  (exchange [this] [this val]
    "Filter the results of request-executions based on theorder exchange.")
  (side [this] [this val]
    "Filter the results of request-executions based on the order action.
     Valid values: :buy, :sell, :sell-short"))

(extend-type com.ib.client.ExecutionFilter
  ExecutionFilter
  (client-id
    ([this] (. this m_clientId))
    ([this val] (set! (. this m_clientId) val)))
  
  (account-code
    ([this] (. this m_acctCode))
    ([this val] (set! (. this m_acctCode) val)))
  
  (after-time
    ([this] (-> (. this m_time) t/translate-from-ib-date-time))
    ([this val] (set! (. this m_time)
                      (t/translate-to-ib-date-time val))))
  
  (order-symbol
    ([this] (. this m_symbol))
    ([this val] (set! (. this m_symbol) val)))

  (security-type
    ([this] (-> (. this m_secType)
                t/translate-from-ib-security-type))
    ([this val] (set! (. this m_secType)
                      (t/translate-to-ib-security-type val))))
  
  (exchange
    ([this] (. this m_exchange))
    ([this val] (set! (. this m_exchange) val)))
  
  (side
    ([this] (-> (. this m_side)
                t/translate-from-ib-order-action))
    ([this val] (set! (. this m_side)
                      (t/translate-to-ib-order-action val)))))

(defn execution-filter
  ([client-id]
     (execution-filter client-id nil nil nil))
  ([client-id account-code order-time symbol]
     (doto (com.ib.client.ExecutionFilter.)
       (.client-id client-id)
       (.account-code account-code)
       (.order-time order-time)
       (.symbol symbol))))