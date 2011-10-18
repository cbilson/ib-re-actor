(ns re-actor.tests.orders
  (:use [re-actor.orders]
        [clj-time.core :only [date-time]]
        [midje.sweet])
  (:import [com.ib.client Order]))

(fact "it has a low level interface to create limit orders"
  (limit-order ..contract.. :buy 100 25.6 42) =>
  (fn [x] (and (= "BUY" (.m_action x))
              (= "LMT" (.m_orderType x))
              (= 100 (.m_totalQuantity x))
              (= 25.6 (.m_lmtPrice x))
              (= "DAY" (.m_tif x))
              (= 42 (.m_clientId x)))))

(fact "it has a higher level interface that makes it easier to create buy limit orders specifically"
  (buy-limit ..contract.. 100 25.6) =>
  (fn [x] (and (= "BUY" (.m_action x))
              (= "LMT" (.m_orderType x))
              (= 100 (.m_totalQuantity x))
              (= 25.6 (.m_lmtPrice x))
              (= "DAY" (.m_tif x)))))

(fact "it lets me set time in force to a logical unit"
  (let [order (-> (Order.) (time-in-force :day))]
    (.m_tif order) => "DAY"))

(fact "it lets me set good till date on orders with just a date"
  (let [order (-> (Order.) (time-in-force (date-time 2011 1 2 3 4)))]
    (.m_tif order) => "GTD"
    (.m_goodTillDate order) => "20110102 03:04:00 UTC"))

(fact "I can set an order good for day"
  (.m_tif (good-for-day (Order.))) => "DAY")
