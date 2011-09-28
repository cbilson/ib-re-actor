(ns re-actor.contracts
  (:use [re-actor.conversions])
  (:import [com.ib.client Contract]))

(defmacro field-based-property
  ([property-name field-name]
     `(defn ~property-name
        ([x#] (. x# ~field-name))
        ([x# val#] (set! (. x# ~field-name) val#) x#)))
  ([property-name field-name get-xform set-xform]
     `(defn ~property-name
        ([x#] (~get-xform (. x# ~field-name)))
        ([x# val#] (set! (. x# ~field-name) (~set-xform val#)) x#))))

(field-based-property expiry m_expiry translate-from-ib-expiry translate-to-ib-expiry)
(field-based-property underlying-symbol m_symbol)
(field-based-property local-symbol m_localSymbol)
(field-based-property multiplier m_multiplier #(Double/parseDouble %) str)
(field-based-property exchange m_exchange)
(field-based-property currency m_currency)
(field-based-property primary-exchange m_primaryExch)
(field-based-property include-expired? m_includeExpired)
(field-based-property contract-id m_conId)
(field-based-property security-id-type m_secIdType translate-from-ib-security-id-type translate-to-ib-security-id-type)
(field-based-property security-id m_secId)

(defn futures-contract
  ([] (let [contract (Contract.)]
        (set! (.m_secType contract) (translate-to-ib-security-type :future))
        contract))
  ([^org.joda.DateTime expiry-val]
     (-> (futures-contract)
         (expiry expiry-val)))
  ([^String symbol-val ^String exchange-val ^org.joda.DateTime expiry-val]
     (-> (futures-contract expiry-val)
         (exchange exchange-val)
         (underlying-symbol symbol-val))))
