(ns re-actor.orders
  (:use [re-actor.conversions])
  (:import [com.ib.client Order]))

(defn id
  ([^Order order] (.m_clientId order))
  ([^Order order value] (set! (.m_clientId order) value)))

(defn quantity
  ([^Order order] (.m_totalQuantity order))
  ([^Order order value] (set! (.m_totalQuantity order) value)))

(defn limit-price
  ([^Order order] (.m_lmtPrice order))
  ([^Order order value] (set! (.m_lmtPrice order) value)))

(def *client-order-id* (atom 1))

(defn limit-order
  "Create a limit order.

A Limit order is an order to buy or sell at a specified price or
better. The Limit order ensures that if the order fills, it will not
fill at a price less favorable than your limit price, but it does not
guarantee a fill.

Specify the contract, action, quantity, and limit-price.

Valid actions are: :buy, :sell, :sell-short."
  ([contract action quantity limit-price]
     (limit-order contract action quantity limit-price (swap! *client-order-id* inc)))
  ([contract action quantity limit-price id]
     (let [order (Order.)]
       (set! (.m_action order) (translate-order-action action))
       (set! (.m_orderType order) (translate-to-ib-order-type :limit))
       (set! (.m_tif order) (translate-time-in-force :day))
       (re-actor.orders/id order id)
       (re-actor.orders/quantity order quantity)
       (re-actor.orders/limit-price order limit-price)
       order)))

(defmulti time-in-force
  "Change the time an order will be in force if not filled."
  (fn [_ value] (class value)))
(defmethod time-in-force clojure.lang.Keyword [^Order order value]
  (set! (.m_tif order) (translate-time-in-force value))
  order)
(defmethod time-in-force org.joda.time.DateTime [^Order order date]
  (set! (.m_goodTillDate order) (translate-to-ib-date-time date))
  (time-in-force order :good-till-date))

(defn good-for-day [^Order order]
  "Make an order be in force for the rest of the day or until filled."
  (time-in-force order :day))

(defn buy-limit
  "Create a limit order to buy a contract."
  ([contract quantity limit-price transmit? id]
     (limit-order contract :buy quantity limit-price transmit? id))
  ([contract quantity limit-price transmit?]
     (limit-order contract :buy quantity limit-price transmit?))
  ([contract quantity limit-price]
     (limit-order contract :buy quantity limit-price)))
