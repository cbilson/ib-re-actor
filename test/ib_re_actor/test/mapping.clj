(ns re-actor.test.mapping
  (:use [ib-re-actor.mapping]
        [clj-time.core :only [date-time]]
        [midje.sweet])
  (:import [com.ib.client Contract ContractDetails Order]))

(def con (Contract.))
(do (set! (.m_expiry con) "201109"))

(future-fact "I can get and set the attributes of contracts"
  (expiry (expiry con (date-time 2011 12))) => (date-time 2011 12)
  (underlying-symbol (underlying-symbol con "foo")) => "foo"
  (local-symbol (local-symbol con "bar")) => "bar"
  (multiplier (multiplier con 42.5)) => 42.5
  (exchange (exchange con "NYBOT")) => "NYBOT"
  (currency (currency con "USD")) => "USD"
  (primary-exchange (primary-exchange con "GLOBEX")) => "GLOBEX"
  (include-expired? (include-expired? con true)) => true
  (contract-id (contract-id con 54321)) => 54321
  (security-id-type (security-id-type con :isin)) => :isin
  (security-id (security-id con "US1234567890")) => "US1234567890")

(future-fact "I can create futures contracts"
  (let [con (futures-contract "TF" "NYBOT" (date-time 2011 12))]
    con => (fn [x] (= (date-time 2011 12) (expiry x)))
    con => (fn [x] (= "TF" (underlying-symbol x)))
    con => (fn [x] (= "NYBOT" (exchange x)))))

(future-fact "I can create index contracts"
  (let [con (index "TF" "NYBOT")]
    con => (fn [x] (= "TF" (underlying-symbol x)))
    con => (fn [x] (= "NYBOT" (exchange x)))))

(future-fact "it has a low level interface to create limit orders"
  (limit-order ..contract.. :buy 100 25.6 42) =>
  (fn [x] (and (= "BUY" (.m_action x))
              (= "LMT" (.m_orderType x))
              (= 100 (.m_totalQuantity x))
              (= 25.6 (.m_lmtPrice x))
              (= "DAY" (.m_tif x))
              (= 42 (.m_clientId x)))))

(future-fact "it has a higher level interface that makes it easier to create buy limit orders specifically"
  (buy-limit ..contract.. 100 25.6) =>
  (fn [x] (and (= "BUY" (.m_action x))
              (= "LMT" (.m_orderType x))
              (= 100 (.m_totalQuantity x))
              (= 25.6 (.m_lmtPrice x))
              (= "DAY" (.m_tif x)))))

(future-fact "it lets me set time in force to a logical unit"
  (let [order (-> (Order.) (time-in-force :day))]
    (.m_tif order) => "DAY"))

(future-fact "it lets me set good till date on orders with just a date"
  (let [order (-> (Order.) (time-in-force (date-time 2011 1 2 3 4)))]
    (.m_tif order) => "GTD"
    (.m_goodTillDate order) => "20110102 03:04:00 UTC"))

(future-fact "I can set an order good for day"
  (.m_tif (good-for-day (Order.))) => "DAY")
