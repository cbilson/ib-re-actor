(ns ib-re-actor.contracts
  (:require [ib-re-actor.util :refer [field-props]]
            [clj-time.core :as time]
            [clojure.pprint :as pp]))

(defprotocol Contract
  (contract-id [this] [this val]
    "The unique contract identifier.")
  (underlying-symbol [this] [this val]
    "This is the symbol of the underlying asset.")
  (exchange [this] [this val]
    "The order destination, such as Smart.")
  (local-symbol [this] [this val]
    "This is the local exchange symbol of the underlying asset.")
  (primary-exchange [this] [this val]
    "Identifies the listing exchange for the contract (do not list SMART).")
  (currency [this] [this val]
    "Specifies the currency. Ambiguities may require that this field be specified,
     for example, when SMART is the exchange and IBM is being requested (IBM can
     trade in GBP or USD). Given the existence of this kind of ambiguity, it is a
     good idea to always specify the currency.")
  (security-type [this] [this val]
    "This is the security type. Valid values are: STK, OPT, FUT, IND, FOP, CASH, BAG")
  (include-expired? [this] [this val]
    "If set to true, contract details requests and historical data queries can be
     performed pertaining to expired contracts.

     Note: Historical data queries on expired contracts are limited to the last
     year of the contracts life, and are initially only supported for expired
     futures contracts,")
  (security-id-type [this] [this val]
    "Security identifier, when querying contract details or when placing orders.
     Supported identifiers are: ISIN (Example: Apple: US0378331005), CUSIP
     (Example: Apple: 037833100), SEDOL (Consists of 6-AN + check digit.
     Example: BAE: 0263494), RIC (Consists of exchange-independent RIC Root and
     a suffix identifying the exchange. Example: AAPL.O for Apple on NASDAQ.)")
  (security-id [this] [this val]
    "Unique identifier for the secIdType."))

(defprotocol ComboContract
  (combo-legs-description [this] [this val]
    "Description for combo legs")
  (combo-legs [this] [this val]
    "Dynamic memory structure used to store the leg definitions for this contract."))

(defprotocol FixedIncomeContract
  (put-call-right [this] [this val]
    "Specifies a Put or Call. Valid values are: P, PUT, C, CALL."))

(defprotocol DerivativeContract
  (expiry [this] [this val]
    "The expiration date.")
  (multiplier [this] [this val]
    "Allows you to specify a future or option contract multiplier. This is only
     necessary when multiple possibilities exist."))

(defprotocol OptionContract
  (strike [this] [this val]
    "The strike price."))

(extend com.ib.client.Contract
  Contract
  (field-props
   [contract-id m_conId]
   [underlying-symbol m_symbol]
   [exchange m_exchange]
   [local-symbol m_localSymbol]
   [primary-exchange m_primaryExch]
   [currency m_currency]
   [security-type m_secType :translation :security-type]
   [include-expired? m_includeExpired]
   [security-id-type m_secIdType :translation :security-id-type]
   [security-id m_secId])

  ComboContract
  (field-props
   [combo-legs-description m_comboLegsDescrip])

  DerivativeContract
  (field-props
   [expiry m_expiry :translation :expiry]
   [multiplier m_multiplier :translation :double-string]))

(defn pprint-contract [contract]
  (pp/cl-format true "#<Contract{~A ~A, ~A}>"
                (name (security-type contract))
                (local-symbol contract)
                (exchange contract)))

(.addMethod pp/simple-dispatch com.ib.client.Contract pprint-contract)

(defn futures-contract
  ([]
     (doto (com.ib.client.Contract.)
       (security-type :future)))
  ([local-symbol-val exchange-val]
     (doto (futures-contract)
       (local-symbol local-symbol-val)
       (exchange exchange-val)))
  ([symbol-val exchange-val expiry-val]
     (doto (futures-contract)
       (exchange exchange-val)
       (underlying-symbol symbol-val)
       (expiry expiry-val))))

(defn index
  ([]
     (doto (com.ib.client.Contract.)
       (security-type :index)))
  ([symbol-val exchange-val]
     (doto (index)
       (underlying-symbol symbol-val)
       (exchange exchange-val))))

(defn equity
  ([]
     (doto (com.ib.client.Contract.)
       (security-type :equity)))
  ([local-symbol-val exchange-val]
     (doto (equity)
       (local-symbol local-symbol-val)
       (exchange exchange-val))))
