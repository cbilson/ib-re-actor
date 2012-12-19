(ns ib-re-actor.test.mapping
  (:use [ib-re-actor.mapping]
        [clj-time.core :only [date-time]]
        [midje.sweet])
  (:import [com.ib.client
            Contract ContractDetails
            Order OrderState
            Execution ExecutionFilter
            CommissionReport]))

(defn invoke-private-ctor [type]
  (let [ctor (first (.getDeclaredConstructors type))]
    (.setAccessible ctor true)
    (.newInstance ctor nil)))

(defmacro defmappingtest
  "Checking all these fields is pretty tedious, but important and defining the mappings
is error-prone, so I think it's important that we test them. To relieve the tedium, I
will just define a macro that generates the tests in the form I was using before.

Specs are a vector of:
  <<map key>>
  <<field name>>
  <<expected map value>>
  <<expected object value (or omit if same as expected map value)>>

You can have duplicate rows for the same field<->map key to try out different values"
  [type & specs]
  (let [obj (gensym "object")
        map-obj (gensym "map")
        [options specs] (split-with keyword? specs)
        options (set options)
        ctor (if (options :private-constructor)
               `(invoke-private-ctor ~type)
               `(new ~type))]
    `(do
    ;;; it's kind of handy to be able to look at an example sometimes
       (def ~(symbol (str "example-" type))
         (let [~obj ~ctor]
           ~@(for [[k field mv ov] specs]
               `(set! (. ~obj ~field) ~(or ov mv)))
           ~obj))

       (def ~(symbol (str "example-" type "-map"))
         ~(zipmap (map #(% 0) specs)
                  (map #(% 2) specs)))

       (fact ~(str "mapping " type)
             (fact "object -> map"
                   (let [~obj ~ctor]
                     ~@(for [[k field mv ov] specs]
                         `(set! (. ~obj ~field) ~(or ov mv)))
                     (let [~map-obj (->map ~obj)]
                       ~@(for [[k field mv _] specs]
                           `(fact ~(str field " maps to " k)
                                  (~map-obj ~k) => ~mv)))))
             ~(when (not (options :private-constructor))
                `(fact "map -> object"
                      (let [~map-obj ~(zipmap (map #(% 0) specs)
                                              (map #(% 2) specs))
                            ~obj (map-> ~type ~map-obj)]
                        ~@(for [[k field mv ov] specs]
                            `(fact ~(str k " maps to " field)
                                   (. ~obj ~field) => ~(or ov mv))))))))))

(comment
  (clojure.pprint/pprint (macroexpand-1 '(defmappingtest Thingy [:id m_id 1 "foo"])))
  (clojure.pprint/pprint (macroexpand-1 '(defmappingtest Thingy
                                           :private-constructor
                                           [:id m_id 1 "foo"])))
  )


(defmappingtest Contract
  [:contract-id m_conId 1]
  [:symbol m_symbol "some symbol"]
  [:exchange m_exchange "some exchange"]
  [:local-symbol m_localSymbol "some local symbol"]
  [:primary-exchange m_primaryExch "some primary exchange"]
  [:currency m_currency "some currency"]
  [:type m_secType :equity "STK"]
  [:include-expired? m_includeExpired true]
  [:security-id-type m_secIdType :isin "ISIN"]
  [:security-id m_secId "AB1234567890"]
  [:combo-legs-description m_comboLegsDescrip "some description of combo legs"]
  [:expiry m_expiry (date-time 2000 1) "200001"]
  [:multiplier m_multiplier 234.567 "234.567"]
  [:put-call-right m_right :put "PUT"])

(defmappingtest ContractDetails
  ;; TODO: figure out what to do about m_summary
  [:market-name m_marketName "some market name"]
  [:trading-class m_tradingClass "some trading class"]
  [:min-tick m_minTick 2.]
  [:price-magnifier m_priceMagnifier 4]
  [:order-types m_orderTypes [:limit :market] "LMT,MKT"]
  [:valid-exchanges m_validExchanges ["GLOBEX" "ECBOT"] "GLOBEX,ECBOT"]
  [:underlying-contract-id m_underConId 9876]
  [:long-name m_longName "some long name"]
  [:cusip m_cusip "459200101"]
  [:ratings m_ratings "some credit ratings"]
  [:description-details m_descAppend "some more description stuff"]
  [:bond-type m_bondType "some bond type"]
  [:coupon-type m_couponType "some coupon type"]
  [:callable? m_callable true]
  [:putable? m_putable true]
  [:coupon m_coupon 1.23]
  [:convertible? m_convertible true]
  [:maturity m_maturity (date-time 2020 2 3) "02/03/2020"]
  [:issue-date m_issueDate (date-time 2010 4 5) "04/05/2010"]
  [:next-option-date m_nextOptionDate (date-time 2013 5 6) "05/06/2013"]
  [:next-option-type m_nextOptionType "some option type"]
  [:next-option-partial m_nextOptionPartial true]
  [:notes m_notes "some notes"]
  [:contract-month m_contractMonth "some contract month"]
  [:industry m_industry "some industry"]
  [:category m_category "some category"]
  [:subcategory m_subcategory "some sub-category"]
  [:time-zone-id m_timeZoneId "some time zone id"]
  [:trading-hours m_tradingHours "some trading hours"]
  [:liquid-hours m_liquidHours "some liquid hours"])

(defmappingtest Execution
  [:account-code m_acctNumber "some account number"]
  [:average-price m_avgPrice 23.45]
  [:client-id m_clientId 1]
  [:cummulative-quantity m_cumQty 2]
  [:exchange m_exchange "some exchange"]
  [:execution-id m_execId "some execution id"]
  [:liquidate-last m_liquidation 10]
  [:order-id m_orderId 4]
  [:permanent-id m_permId 5]
  [:price m_price 6.78]
  [:shares m_shares 9]
  [:side m_side :buy "BOT"]
  [:time m_time "some time"])

(defmappingtest ExecutionFilter
  [:client-id m_clientId 1]
  [:account-code m_acctCode "some account code"]

  ;; TODO: this should be a date-time
  [:after-time m_time "20000102-23:59:59 UTC"]
  [:order-symbol m_symbol "some symbol"]
  [:security-type m_secType :equity "STK"]
  [:exchange m_exchange "GLOBEX"]
  [:side m_side :buy "BUY"])

(defmappingtest Order
  [:order-id m_orderId 1]
  [:client-id m_clientId 2]
  [:permanent-id m_permId 3]
  [:transmit? m_transmit true]
  [:quantity m_totalQuantity 4]
  [:action m_action :buy "BUY"]
  [:type m_orderType :limit "LMT"]
  [:block-order? m_blockOrder true]
  [:sweep-to-fill? m_sweepToFill true]
  [:time-in-force m_tif :day "DAY"]
  [:good-after-time m_goodAfterTime "20121213 23:59:59"]
  [:good-till-date m_goodTillDate "20121214 23:59:59"]
  [:outside-regular-trading-hours? m_outsideRth true]
  [:hidden? m_hidden true]
  [:all-or-none? m_allOrNone true]
  [:limit-price m_lmtPrice 23.99]
  [:discretionary-amount m_discretionaryAmt 1.99]
  [:stop-price m_auxPrice 24.99])

(defmappingtest OrderState :private-constructor
  [:status m_status :filled "Filled"]
  [:initial-margin m_initMargin "23.45"]
  [:maintenance-margin m_maintMargin "34.56"]
  [:equity-with-loan m_equityWithLoan "45.67"]
  [:commission m_commission 56.78]
  [:minimum-commission m_minCommission 67.89]
  [:maximum-commission m_maxCommission 78.90]
  [:commission-currency m_commissionCurrency "ABC"]
  [:warning-text m_warningText "some warning text"])

(defmappingtest CommissionReport :private-constructor
  [:commission m_commission 23.45]
  [:currency m_currency "some currency code"]
  [:execution-id m_execId "some execution id"]
  [:realized-profit-loss m_realizedPNL 34.56]
  [:yield m_yield 4.56]
  [:yield-redemption-date m_yieldRedemptionDate 20101031])
