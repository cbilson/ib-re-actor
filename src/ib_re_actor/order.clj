(ns ib-re-actor.order
  (:use [ib-re-actor.translation :only [translate]]
        [ib-re-actor.util :only [field-props]]
        [clojure.pprint :only [cl-format simple-dispatch]])
  (:require [clj-time.format :as tf]))

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

  (good-till-date [this]
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

(extend com.ib.client.Order
  Order
  (field-props
   [order-id m_orderId]
   [client-id m_clientId]
   [permanent-id m_permId]
   [transmit? m_transmit]
   [quantity m_totalQuantity]
   [action m_action :translation :order-action]
   [order-type m_orderType :translation :order-type]
   [block-order? m_blockOrder]
   [sweep-to-fill? m_sweepToFill]
   [time-in-force m_tif :translation :time-in-force]
   [good-after-time m_goodAfterTime]
   [good-till-date m_goodTillDate]
   [outside-regular-trading-hours? m_outsideRth]
   [hidden? m_hidden]
   [all-or-none? m_allOrNone ])

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

(defmethod simple-dispatch com.ib.client.Order [order]
  (cl-format true "#<Order{id ~A ~A ~A ~A}>"
             (order-id order)
             (order-type order)
             (action order)
             (quantity order)))
