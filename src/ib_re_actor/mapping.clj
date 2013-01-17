(ns ib-re-actor.mapping
  "Functions for mapping to and from Interactive Brokers classes. It is much easier to work
with maps in clojure, so we use these functions internally on all the data we exchange
with the Interactive Brokers API.

In addition to just converting to maps, we also use these functions to translate some
primitives: strings with constant values into keywords, booleans in strings into booleans,
date strings into clj-time dates, etc."
  (:use [clojure.string :only [join]]
        [ib-re-actor.translation :only [translate]]))

(defprotocol Mappable
  (->map [this]
    "Create a map with the all the non-the non-null properties of object."))

(defmulti map-> (fn [type _] type))

(defn- assoc-if-val-non-nil
  "Chainable, conditional assoc. If v is not nil, assoc it and return the result,
otherwise, don't and return m unchanged."
  ([m k v]
     (if (nil? v) m (assoc m k v)))
  ([m k v translation]
     (if (nil? v) m (assoc m k (translate :from-ib translation v)))))

(defn- assoc-nested [m k v]
  (if (nil? v) m (assoc m k (->map v))))

(defn emit-map<-field
  "When mapping from an object to a clojure map, this creates a call to assoc in the value.
optional parameters:

   :translation <<translation key from translation.clj>>:
      Specifying this option will add a call to (translate to-from ...) in each field
setter or assoc when mapping to and from objects.

   :nested <<type>>:
      Specifying this will map a nested instance of another class."
  [this [k field & options]]
  (let [{:keys [translation nested]} (apply hash-map options)
        m (gensym "m")]
    (cond
     (not (nil? translation)) `((assoc-if-val-non-nil ~k (. ~this ~field) ~translation))
     (not (nil? nested)) `((assoc-nested ~k (. ~this ~field)))
     :otherwise `((assoc-if-val-non-nil ~k (. ~this ~field))))))

(defn emit-map->field
  "When mapping from a clojure map to an object, this creates a call to set the associated
field on the object."
  [m this [key field & options]]
  (let [{:keys [translation nested]} (apply hash-map options)
        val (gensym "val")]
    `((if-let [~val (~key ~m)]
        (try
          (set! (. ~this ~field)
                ~(cond
                  (not (nil? translation)) `(translate :to-ib ~translation ~val)
                  (not (nil? nested)) `(map-> ~nested ~val)
                  :otherwise `~val))
          (catch ClassCastException ex#
            (throw (ex-info (str "Failed to map field " ~(str field)
                                 ~(when translation
                                    (str ", using translation " translation))
                                 ", value \"" ~val "\"")
                            {:class (class ~this)
                             :key ~key
                             :field ~(str field)
                             :translation ~translation}
                            ex#))))))))

(defmacro defmapping
  "This is used to extend an Interactive Brokers API class with a method to convert it into
a clojure map, and using the same information, add a method to the map-> multimethod to
convert maps into instances of the IB class."
  [c & field-keys]
  (let [this (gensym "this")
        field-map (gensym "field-map")
        valid-keys (gensym "valid-keys")]
    `(do
       (extend-type ~c
         Mappable
         (->map [~this]
           (-> {} ~@(mapcat (partial emit-map<-field this) field-keys))))

       (defmethod map-> ~c [_# ~field-map]
         (let [~this (new ~c)]
           ~@(mapcat (partial emit-map->field field-map this) field-keys)
           ~this)))))

(defmacro defmapping-readonly
  "Same as defmapping, but for classes that don't have public constructors. Since we can't
create instances, we will only map from objects to clojure maps."
  [c & field-keys]
  (let [this (gensym "this")]
    `(extend-type ~c
       Mappable
       (->map [~this]
         (-> {} ~@(mapcat (partial emit-map<-field this) field-keys))))))

(defmapping com.ib.client.Contract
  [:contract-id m_conId]
  [:symbol m_symbol]
  [:exchange m_exchange]
  [:local-symbol m_localSymbol]
  [:primary-exchange m_primaryExch]
  [:currency m_currency]
  [:type m_secType :translation :security-type]
  [:include-expired? m_includeExpired]
  [:security-id-type m_secIdType :translation :security-id-type]
  [:security-id m_secId]
  [:combo-legs-description m_comboLegsDescrip]
  [:expiry m_expiry :translation :expiry]
  [:multiplier m_multiplier :translation :double-string]
  [:put-call-right m_right :translation :right])

(defmapping com.ib.client.ContractDetails
  [:summary m_summary :nested com.ib.client.Contract]
  [:market-name m_marketName]
  [:trading-class m_tradingClass]
  [:min-tick m_minTick]
  [:price-magnifier m_priceMagnifier]
  [:order-types m_orderTypes :translation :order-types]
  [:valid-exchanges m_validExchanges :translation :exchanges]
  [:underlying-contract-id m_underConId]
  [:long-name m_longName]
  [:cusip m_cusip]
  [:ratings m_ratings]
  [:description-details m_descAppend]
  [:bond-type m_bondType]
  [:coupon-type m_couponType]
  [:callable? m_callable]
  [:putable? m_putable]
  [:coupon m_coupon]
  [:convertible? m_convertible]
  [:maturity m_maturity :translation :date]
  [:issue-date m_issueDate :translation :date]
  [:next-option-date m_nextOptionDate :translation :date]
  [:next-option-type m_nextOptionType]
  [:next-option-partial m_nextOptionPartial]
  [:notes m_notes]
  [:contract-month m_contractMonth]
  [:industry m_industry]
  [:category m_category]
  [:subcategory m_subcategory]
  [:time-zone-id m_timeZoneId]
  [:trading-hours m_tradingHours]
  [:liquid-hours m_liquidHours])

(defmapping com.ib.client.ExecutionFilter
  [:client-id m_clientId]
  [:account-code m_acctCode]
  [:after-time m_time :translation :timestamp]
  [:order-symbol m_symbol]
  [:security-type m_secType :translation :security-type]
  [:exchange m_exchange]
  [:side m_side :translation :order-action])

(defmapping com.ib.client.Execution
  [:account-code m_acctNumber]
  [:average-price m_avgPrice]
  [:client-id m_clientId]
  [:cummulative-quantity m_cumQty]
  [:exchange m_exchange]
  [:execution-id m_execId]
  [:liquidate-last m_liquidation]
  [:order-id m_orderId]
  [:permanent-id m_permId]
  [:price m_price]
  [:shares m_shares]
  [:side m_side :translation :execution-side]
  [:time m_time])

(defmapping com.ib.client.Order
  [:order-id m_orderId]
  [:client-id m_clientId]
  [:permanent-id m_permId]
  [:transmit? m_transmit]
  [:quantity m_totalQuantity]
  [:action m_action :translation :order-action]
  [:type m_orderType :translation :order-type]
  [:block-order? m_blockOrder]
  [:sweep-to-fill? m_sweepToFill]
  [:time-in-force m_tif :translation :time-in-force]
  [:good-after-time m_goodAfterTime]
  [:good-till-date m_goodTillDate]
  [:outside-regular-trading-hours? m_outsideRth]
  [:hidden? m_hidden]
  [:all-or-none? m_allOrNone ]
  [:limit-price m_lmtPrice]
  [:discretionary-amount m_discretionaryAmt]
  [:stop-price m_auxPrice])

(defmapping-readonly com.ib.client.OrderState
  [:status m_status :translation :order-status]
  [:initial-margin m_initMargin]
  [:maintenance-margin m_maintMargin]
  [:equity-with-loan m_equityWithLoan]
  [:commission m_commission]
  [:minimum-commission m_minCommission]
  [:maximum-commission m_maxCommission]
  [:commission-currency m_commissionCurrency]
  [:warning-text m_warningText])

(defmapping-readonly com.ib.client.CommissionReport
  [:commission m_commission]
  [:currency m_currency]
  [:execution-id m_execId]
  [:realized-profit-loss m_realizedPNL]
  [:yield m_yield]
  [:yield-redemption-date m_yieldRedemptionDate :translate :yield-redemption-date])