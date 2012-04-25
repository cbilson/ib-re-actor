(ns ib-re-actor.order-state
  (:use [ib-re-actor.util :only [field-props]]
        [clojure.pprint :only [simple-dispatch pprint]]))

(defprotocol OrderState
  (status [this]
    "The order status")
  (initial-margin [this]
    "The impact the order would have on initial margin")
  (maintenance-margin [this]
    "The impact the order would have on maintenance margin")
  (equity-with-loan [this]
    "The impact the order would have on equity with loan")
  (commission [this]
    "The commission amount for the order")
  (minimum-commission [this]
    "Used in conjunction with maximum-commission, this defines the lowest
     end of the possible range into which the actual order commission will
     fall.")
  (maximum-commission [this]
    "Used in conjunction with minimum-commission field, this defines the
     highest end of the possible range into which the actual order commission
     will fall.")
  (commission-currency [this]
    "The currency of the commission value")
  (warning-text [this]
    "A warning message if warranted"))

(extend com.ib.client.OrderState
  OrderState
  (field-props
   [status m_status :read-only :translation :order-status]
   [initial-margin m_initMargin :read-only]
   [maintenance-margin m_maintMargin :read-only]
   [equity-with-loan m_equityWithLoan :read-only]
   [commission m_commission :read-only]
   [minimum-commission m_minCommission :read-only]
   [maximum-commission m_maxCommission :read-only]
   [commission-currency m_commissionCurrency :read-only]
   [warning-text m_warningText :read-only]))

(defn order-status [kw-attribs])

(defmethod simple-dispatch com.ib.client.Order [order-status]
  (print "#ib-re-actor.order-status ")
  (pprint {:status (status order-status)
           :initial-margin (initial-margin order-status)
           :maintenance-margin (maintenance-margin order-status)
           :equity-with-loan (equity-with-loan order-status)
           :commission (commission order-status)
           :minimum-commission (minimum-commission order-status)
           :maximum-commission (maximum-commission order-status)
           :commission-currency (commission-currency order-status)
           :warning-text (warning-text order-status)}))