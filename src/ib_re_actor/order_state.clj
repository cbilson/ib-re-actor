(ns ib-re-actor.order-state
  (:use [ib-re-actor.util :only [field-props Mappable to-map assoc-if]]
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
   [warning-text m_warningText :read-only])

  Mappable
  {:to-map (fn [this]
             (-> {}
                 (assoc-if :status (status this))
                 (assoc-if :initial-margin (initial-margin this))
                 (assoc-if :maintenance-margin (maintenance-margin this))
                 (assoc-if :equity-with-loan (equity-with-loan this))
                 (assoc-if :commission (commission this))
                 (assoc-if :minimum-commission (minimum-commission this))
                 (assoc-if :maximum-commission (maximum-commission this))
                 (assoc-if :commission-currency (commission-currency this))
                 (assoc-if :warning-text (warning-text this))))})

;; TODO: find constructor for com.ib.client.OrderState
#_(defn map->orders-status [m]
  (let [this (com.ib.client.OrderState.)]
    (status this (:status m))
    (initial-margin this (:initial-margin m))
    (maintenance-margin this (:maintenance-margin m))
    (equity-with-loan this (:equity-with-loan m))
    (commission this (:commission m))
    (minimum-commission this (:minimum-commission m))
    (maximum-commission this (:maximum-commission m))
    (commission-currency this (:commission-currency m))
    (warning-text this (:warning-text m))))

(defmethod simple-dispatch com.ib.client.OrderState [order-state]
  (print "#ib-re-actor.order-state ")
  (pprint (to-map order-state)))
