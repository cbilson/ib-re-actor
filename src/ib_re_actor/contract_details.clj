(ns ib-re-actor.contract-details
  (:use [ib-re-actor.util :only [field-props assoc-if Mappable to-map]]
        [clojure.pprint :only [simple-dispatch cl-format pprint]]))

(defprotocol ContractDetails
  (summary [this] [this val]
    "A contract summary.")
  (market-name [this] [this val]
    "The market name for this contract.")
  (trading-class [this] [this val]
    "The trading class name for this contract.")
  (min-tick [this] [this val]
    "The minimum price tick.")
  (price-magnifier [this] [this val]
    "Allows execution and strike prices to be reported consistently with market data,
     historical data and the order price, i.e. Z on LIFFE is reported in index points
     and not GBP.")
  (order-types [this] [this val]
    "The list of valid order types for this contract.")
  (valid-exchanges [this] [this val]
    "The list of exchanges this contract is traded on.")
  (underlying-contract-id [this] [this val]
    "The underlying contract ID.")
  (long-name [this] [this val]
    "Descriptive name of the asset.")
  (cusip [this] [this val]
    "For Bonds. The nine-character bond CUSIP or the 12-character SEDOL.")
  (ratings [this] [this val]
    "For Bonds. Identifies the credit rating of the issuer. A higher credit rating generally indicates a less risky investment. Bond ratings are from Moody's and S&P respectively.")
  (description-details [this] [this val]
    "For Bonds. A description string containing further descriptive information about the bond.")
  (bond-type [this] [this val]
    "For Bonds. The type of bond, such as CORP.")
  (coupon-type [this] [this val]
    "For Bonds. The type of bond coupon.")
  (callable? [this] [this val]
    "For Bonds. Values are True or False. If true, the bond can be called by the issuer under certain conditions.")
  (putable? [this] [this val]
    "For Bonds. Values are True or False. If true, the bond can be sold back to the issuer under certain conditions.")
  (coupon [this] [this val]
    "For Bonds. The interest rate used to calculate the amount you will receive in interest payments over the course of the year.")
  (convertible? [this] [this val]
    "For Bonds. Values are True or False. If true, the bond can be converted to stock under certain conditions.")
  (maturity [this] [this val]
    "For Bonds. The date on which the issuer must repay the face value of the bond.")
  (issue-date [this] [this val]
    "For Bonds. The date the bond was issued.")
  (next-option-date [this] [this val]
    "For Bonds, only if bond has embedded options.")
  (next-option-type [this] [this val]
    "For Bonds, only if bond has embedded options.")
  (next-option-partial [this] [this val]
    "For Bonds, only if bond has embedded options.")
  (notes [this] [this val]
    "For Bonds, if populated for the bond in IB's database")
  (contract-month [this] [this val]
    "The contract month. Typically the contract month of the underlying for a futures contract.")
  (industry [this] [this val]
    "The industry classification of the underlying/product. For example, Financial.")
  (category [this] [this val]
    "The industry category of the underlying. For example, InvestmentSvc.")
  (subcategory [this] [this val]
    "The industry subcategory of the underlying. For example, Brokerage.")
  (time-zone-id [this] [this val]
    "The ID of the time zone for the trading hours of the product. For example, EST.")
  (trading-hours [this] [this val]
    "The trading hours of the product. For example, 20090507:0700-1830,1830-2330;20090508:CLOSED.")
  (liquid-hours [this] [this val]
    "The liquid trading hours of the product. For example, 20090507:0930-1600;20090508:CLOSED."))

(extend com.ib.client.ContractDetails
  ContractDetails
  (field-props
   [summary m_summary]
   [market-name m_marketName]
   [trading-class m_tradingClass]
   [min-tick m_minTick]
   [price-magnifier m_priceMagnifier]
   [order-types m_orderTypes :translation :order-types]
   [valid-exchanges m_validExchanges]
   [underlying-contract-id m_underConId]
   [long-name m_longName]
   [cusip m_cusip]
   [ratings m_ratings]
   [description-details m_descAppend]
   [bond-type m_bondType]
   [coupon-type m_couponType]
   [callable? m_callable]
   [putable? m_putable]
   [coupon m_coupon]
   [convertible? m_convertible]
   [maturity m_maturity :translation :date]
   [issue-date m_issueDate :translation :date]
   [next-option-date m_nextOptionDate :translation :date]
   [next-option-type m_nextOptionType]
   [next-option-partial m_nextOptionPartial]
   [notes m_notes]
   [contract-month m_contractMonth]
   [industry m_industry]
   [category m_category]
   [subcategory m_subcategory]
   [time-zone-id m_timeZoneId]
   [trading-hours m_tradingHours]
   [liquid-hours m_liquidHours])

  Mappable
  {:to-map (fn [this]
             (-> {}
                 (assoc-if :summary (-> (summary this) to-map))
                 (assoc-if :market-name (market-name this))
                 (assoc-if :trading-class (trading-class this))
                 (assoc-if :min-tick (min-tick this))
                 (assoc-if :price-magnififer (price-magnifier this))
                 (assoc-if :order-types (order-types this))
                 (assoc-if :valid-exchanges (valid-exchanges this))
                 (assoc-if :underlying-contract-id (underlying-contract-id this))
                 (assoc-if :long-name (long-name this))
                 (assoc-if :cusip (cusip this))
                 (assoc-if :ratings (ratings this))
                 (assoc-if :description-details (description-details this))
                 (assoc-if :bond-type (bond-type this))
                 (assoc-if :coupon-type (coupon-type this))
                 (assoc-if :callable? (callable? this))
                 (assoc-if :putable? (putable? this))
                 (assoc-if :coupon (coupon this))
                 (assoc-if :convertible? (convertible? this))
                 (assoc-if :maturity (maturity this))
                 (assoc-if :issue-date (issue-date this))
                 (assoc-if :next-option-date (next-option-date this))
                 (assoc-if :next-option-type (next-option-type this))
                 (assoc-if :next-option-partial (next-option-partial this))
                 (assoc-if :notes (notes this))
                 (assoc-if :contract-month (contract-month this))
                 (assoc-if :industry (industry this))
                 (assoc-if :category (category this))
                 (assoc-if :subcategory (subcategory this))
                 (assoc-if :time-zone-id (time-zone-id this))
                 (assoc-if :trading-hours (trading-hours this))
                 (assoc-if :liquid-hours (liquid-hours this))))})

(defmethod simple-dispatch com.ib.client.ContractDetails [contract]
  (let [w (java.io.StringWriter.)]
    (pprint (summary contract) w)
    (cl-format true "#<ContractDetails{id ~A, ~A}>"
               (underlying-contract-id contract)
               (.toString w))))
