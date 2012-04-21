(ns ib-re-actor.execution-filter
  (:use [ib-re-actor.util :only [field-props]])
  (:require [clojure.pprint :refer [simple-dispatch cl-format]]))

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

(extend com.ib.client.ExecutionFilter
  ExecutionFilter
  (field-props
    [client-id m_clientId]
    [account-code m_acctCode]
    [after-time m_time :translation :timestamp]
    [order-symbol m_symbol]
    [security-type m_secType :translation :security-type]
    [exchange m_exchange]
    [side m_side :translation :order-action]))

(defn execution-filter
  ([]
     (com.ib.client.ExecutionFilter.))
  ([client-id-val]
     (execution-filter client-id-val nil nil nil))
  ([client-id-val account-code-val order-time-val symbol-val]
     (doto (com.ib.client.ExecutionFilter.)
       (client-id client-id-val)
       (account-code account-code-val)
       (after-time order-time-val)
       (order-symbol symbol-val))))

(defmethod simple-dispatch com.ib.client.ExecutionFilter [val]
  (cl-format true "#<ExecutionFilter{client-id ~A, ~A ~A, after ~A}>"
             (client-id val)
             (or (side val) :all-sides)
             (or (order-symbol val) :all-symbols)
             (or (after-time val) :anytime)))
