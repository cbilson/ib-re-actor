(ns re-actor.test.contracts
  (:use [re-actor.contracts]
        [clj-time.core :only [date-time]]
        [midje.sweet])
  (:import [com.ib.client Contract]))

(def con (Contract.))
(do (set! (.m_expiry con) "201109"))

(fact "I can get and set the attributes of contracts"
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

(fact "I can create futures contracts"
  (let [con (futures-contract "TF" "NYBOT" (date-time 2011 12))]
    con => (fn [x] (= (date-time 2011 12) (expiry x)))
    con => (fn [x] (= "TF" (underlying-symbol x)))
    con => (fn [x] (= "NYBOT" (exchange x)))))

(fact "I can create index contracts"
  (let [con (index "TF" "NYBOT")]
    con => (fn [x] (= "TF" (underlying-symbol x)))
    con => (fn [x] (= "NYBOT" (exchange x)))))
