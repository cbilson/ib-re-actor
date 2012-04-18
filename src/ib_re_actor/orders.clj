(ns ib-re-actor.orders
  (:require [ib-re-actor.translation :as t]
            [ib-re-actor.util :refer [field-props]]
            [clj-time.format :as tf]))

(defprotocol Order
  (order-id [this] [this val])
  (client-id [this] [this val])
  (permanent-id [this])

  (transmit? [this] [this val]
    "Specifies whether the order will be transmitted by TWS. If set to false,
     the order will be created at TWS but will not be sent.")

  (quantity [this] [this val])

  (action [this] [this val]
    "Identifies the side. Valid values are: :buy, :sell, :sell-short.")

  (order-type [this] [this val]
    "Identifies the order type. Valid values are: :market, :market-close, :limit,
     :limit-close, :pegged-to-market, :scale, :stop, :stop-limit, :trail,
     :relative, :VWAP, and :trail-limit")

  (block-order? [this] [this val]
    "If set to true, specifies that the order is an ISE Block order.")

  (sweep-to-fill? [this] [this val]
    "If set to true, specifies that the order is a Sweep-to-Fill order.")

  (time-in-force [this] [this val]
    "The time in force. Valid values are: :day, :good-to-close, :immediate-or-cancel,
     or a date")

  (good-after-time [this] [this val]
    "The trade's \"Good After Time,\"")

  (good-for-day [this]
    "Sets the time-in-force to :day")

  (outside-regular-trading-hours? [this] [this val]
    "If set to true, allows orders to also trigger or fill outside of regular trading hours.")

  (hidden? [this] [this val]
    "If set to true, the order will not be visible when viewing the market depth.
     This option only applies to orders routed to the ISLAND exchange.")

  (all-or-none? [this] [this val]))

(defprotocol LimitOrder
  (limit-price [this] [this val])
  (discretionary-amount [this] [this val]
    "The amount off the limit price allowed for discretionary orders."))

(defprotocol StopLimitOrder
  (stop-price [this] [this val]))

(defprotocol OneCancelsAllOrder
  (one-cancels-all-type [this] [this val]
    "Cancel on Fill with Block = 1 Reduce on Fill with Block = 2 Reduce on Fill
     without Block = 3")
  (one-cancels-all-group [this] [this val]
    "Tells how to handle remaining orders in an OCA group when one order or
     part of an order executes. Valid values include:
     • 1 = Cancel all remaining orders with block
     • 2 = Remaining orders are proportionately reduced in size with block
     • 3 = Remaining orders are proportionately reduced in size with no block
     If you use a value \"with block\" gives your order has overfill protection.
     This means that only one order in the group will be routed at a time to
     remove the possibility of an overfill."))

(defprotocol IcebergOrder
  (display-size [this] [this val]
    "The publicly disclosed order size, used when placing Iceberg orders."))

(defprotocol TriggeredOrder
  (trigger-method [this] [this val]
    "Specifies how Simulated Stop, Stop-Limit and Trailing Stop orders are
     triggered. Valid values are:
     • 0 - The default value. The \"double bid/ask\" method will be used for
           orders for OTC stocks and US options. All other orders will used
           the \"last\" method.
     • 1 - use \"double bid/ask\" method, where stop orders are triggered
           based on two consecutive bid or ask prices.
     • 2 - \"last\" method, where stop orders are triggered based on the last
           price.
     • 3 - double last method.
     • 4 - bid/ask method.
     • 7 - last or bid/ask method.
     • 8 - mid-point method."))

;; TODO: add the rest of the stuff in com.ib.client.Order

(extend com.ib.client.Order
  Order
  (merge
   (field-props
    [order-id m_orderId]
    [client-id m_clientId]
    [transmit? m_transmit]
    [quantity m_totalQuantity]
    [action m_action :translation :order-action]
    [order-type m_orderType :translation :order-type]
    [block-order? m_blockOrder]
    [sweep-to-fill? m_sweepToFill]
    [good-after-time m_goodAfterTime]
    [outside-regular-trading-hours? m_outsideRth]
    [hidden? m_hidden]
    [all-or-none? m_allOrNone ])

   {:permanent-id (fn [this] (.m_permId this))

    :time-in-force
    (fn
      ([this]
         (if (nil? (.m_goodTillDate this))
           (t/translate-from-ib-time-in-force (.m_tif this))
           (t/translate-from-ib-date-time (.m_goodTillDate this))))
      ([this val]
         (if (instance? org.joda.time.DateTime val)
           (set! (.m_goodTillDate this) (t/translate-to-ib-date-time val))
           (set! (.m_tif this) (t/translate-to-ib-time-in-force val)))))

    :good-for-day
    (fn [this] (.time-in-force this :day))})

  LimitOrder
  (field-props
   [limit-price m_lmtPrice]
   [discretionary-amount m_discretionaryAmt])

  StopLimitOrder
  (field-props
   [stop-price m_auxPrice]))

(def ^:dynamic *client-order-id* (atom 1))

(defn limit-order
  "Create a limit order.

A Limit order is an order to buy or sell at a specified price or
better. The Limit order ensures that if the order fills, it will not
fill at a price less favorable than your limit price, but it does not
guarantee a fill.

Specify the contract, action, quantity, and limit-price.

Valid actions are: :buy, :sell, :sell-short."
  ([action-val quantity-val limit-price-val]
     (limit-order action-val quantity-val limit-price-val (swap! *client-order-id* inc)))
  ([action-val quantity-val limit-price-val id]
     (doto (com.ib.client.Order.)
       (action action-val)
       (order-type :limit)
       (time-in-force :day)
       (order-id id)
       (quantity quantity-val)
       (limit-price limit-price-val))))

(defn buy-limit
  "Create a limit order to buy a contract."
  ([contract quantity limit-price transmit? id]
     (limit-order contract :buy quantity limit-price transmit? id))
  ([contract quantity limit-price transmit?]
     (limit-order contract :buy quantity limit-price transmit?))
  ([contract quantity limit-price]
     (limit-order contract :buy quantity limit-price)))

(defn pprint-order [order]
  (clojure.pprint/cl-format true "#<Order{id ~A ~A ~A ~A}>"
                            (order-id order)
                            (order-type order)
                            (action order)
                            (quantity order)))

(.addMethod clojure.pprint/simple-dispatch com.ib.client.Order pprint-order)
