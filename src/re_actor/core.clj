(ns re-actor.core
  (:import [com.ib.client Order]))

(defn market-order
  [contract quantity transmit?]
  (reify
    Order
    (id [this] nil)
    (permanent-id [this] nil)
    (transmit? [this] transmit)))



(defn limit-order
  "Create a limit order.

A Limit order is an order to buy or sell at a specified price or
better. The Limit order ensures that if the order fills, it will not
fill at a price less favorable than your limit price, but it does not
guarantee a fill.

Specify the contract, action, quantity, and limit-price.

Valid actions are: :buy, :sell, :sell-short.
"
  ([contract action quantity limit-price]
     (doto (Order.)
       (set! (. m_clientId) id)
       (set! (. m_action) (translate-order-action action))
       (set! (. m_totalQuantity) (quantity))
       (set! (. m_orderType) "LMT")
       (set! (. m_limitPrice) limit-price)
       (set! (. m_tif) "DAY"))))

(defn time-in-force
  ([^Order order value]
     (set! (.m_tif order) (translate-time-in-force value)))
  ([^Order order value date]
     (doto order
       (set! (.m_tif order) (translate-time-in-force value))
       (set! (.m_goodTillDate (translate-to-ib-date-time date))))))

(defn good-for-day [^Order order]
  (time-in-force order :day))

(defn buy-limit
  ([contract quantity limit-price transmit? id]
     (limit-order contract :buy quantity limit-price transmit? id))
  ([contract quantity limit-price transmit?]
     (limit-order contract :buy quantity limit-price transmit?))
  ([contract quantity limit-price]
     (limit-order contract :buy quantity limit-price)))





