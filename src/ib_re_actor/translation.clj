(ns ib-re-actor.translation
  (:require [clj-time.core :as time]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [clojure.string :as str]))

(defmulti ^:dynamic translate
  "Translate to or from a value from the Interactive Brokers API.

Examples:
user> (translate :to-ib :duration-unit :seconds)
\"S\"
user> (translate :from-ib :duration-unit \"S\")
:second"
  (fn [direction table-name _] [direction table-name]))

(defmulti ^:dynamic valid?
  "Check to see if a given value is an entry in the translation table.

Examples:
user> (valid? :to-ib :duration-unit :s)
false
user> (valid? :to-ib :duration-unit :seconds)
true"
  (fn [direction table-name _] [direction table-name]))

(defmacro translation-table
  "Creates a table for translating to and from string values from the Interactive
Brokers API, as well as translate methods to and from the IB value and a method
to check if if a given value is valid (known)."
  [name vals]
  (let [table-name (keyword name)]
    `(let [to-table# ~vals
           from-table# (zipmap (vals to-table#) (keys to-table#))]

       (def ~name to-table#)

       (defmethod valid? [:to-ib ~table-name] [_# _# val#]
         (contains? to-table# val#))

       (defmethod translate [:to-ib ~table-name] [_# _# val#]
         (when val#
           (cond
            (valid? :to-ib ~table-name val#)
            (to-table# val#)

            (string? val#)
            val#

            :otherwise
            (throw (ex-info (str "Can't translate to IB " ~table-name " " val#)
                            {:value val#
                             :table ~table-name
                             :valid-values (keys to-table#)})))))

       (defmethod valid? [:from-ib ~table-name] [_# _# val#]
         (contains? from-table# val#))

       (defmethod translate [:from-ib ~(keyword name)] [_# _# val#]
         (when val#
           (cond
            (valid? :from-ib ~table-name val#)
            (from-table# val#)

            (string? val#)
            val#

            :otherwise
            (throw (ex-info (str "Can't translate from IB " ~table-name " " val#)
                            {:value val#
                             :table ~table-name
                             :valid-values (vals to-table#)}))))))))

(translation-table duration-unit
                   {:second "S"
                    :seconds "S"
                    :day "D"
                    :days "D"
                    :week "W"
                    :weeks "W"
                    :year "Y"
                    :years "Y"})

(defmethod translate [:to-ib :acceptable-duration] [_ _ [val unit]]
  (case unit
    :second [val :second]
    :seconds [val :seconds]
    :minute [(* 60 val) :seconds]
    :minutes [(* 60 val) :seconds]
    :hour [(* 60 60 val) :seconds]
    :hours [(* 60 60 val) :seconds]
    :day [val :day]
    :days [val :days]
    :week [val :week]
    :weeks [val :weeks]
    :year [val :year]
    :years [val :years]))

(translation-table security-type
                   {:equity "STK"
                    :option "OPT"
                    :future "FUT"
                    :index "IND"
                    :future-option "FOP"
                    :cash "CASH"
                    :bag "BAG"})

(defmethod translate [:to-ib :bar-size-unit] [_ _ unit]
  (case unit
    :second "secs"
    :seconds "secs"
    :minute "min"
    :minutes "mins"
    :hour "hour"
    :hours "hour"
    :day "day"
    :days "days"))

(defmethod translate [:from-ib :bar-size-unit] [_ _ unit]
  (case unit
    "sec" :second
    "secs" :seconds
    "min" :minute
    "mins" :minutes
    "hour" :hour
    "hours" :hours
    "day" :day
    "days" :days))

(translation-table what-to-show
                   {:trades "TRADES"
                    :midpoint "MIDPOINT"
                    :bid "BID"
                    :ask "ASK"
                    :bid-ask "BID_ASK"
                    :historical-volatility "HISTORICAL_VOLATILITY"
                    :option-implied-volatility "OPTION_IMPLIED_VOLATILITY"
                    :option-volume "OPTION_VOLUME"
                    :option-open-interest "OPTION_OPEN_INTEREST"})

(translation-table time-in-force
                   {:day "DAY"
                    :good-to-close "GTC"
                    :immediate-or-cancel "IOC"
                    :good-till-date "GTD"})

(translation-table order-action
                   {:buy "BUY"
                    :sell "SELL"
                    :sell-short "SSHORT"})

(translation-table order-type
                   {:ACTIVETIM "ACTIVETIM"
                    :ADJUST "ADJUST"
                    :ALERT "ALERT"
                    :ALGO "ALGO"
                    :ALGOLTH "ALGOLTH"
                    :ALLOC "ALLOC"
                    :AON "AON"
                    :AUC "AUC"
                    :average-cost "AVGCOST"
                    :basket "BASKET"
                    :BOARDLOT "BOARDLOT"
                    :box-top "BOXTOP"
                    :COND "COND"
                    :CONDORDER "CONDORDER"
                    :CONSCOST "CONSCOST"
                    :DARKPOLL "DARKPOLL"
                    :DAY "DAY"
                    :DEACT "DEACT"
                    :DEACTDIS "DEACTDIS"
                    :DEACTEOD "DEACTEOD"
                    :DIS "DIS"
                    :EVRULE "EVRULE"
                    :FOK "FOK"
                    :good-after-time "GAT"
                    :good-till-date "GTD"
                    :good-till-canceled "GTC"
                    :GTT "GTT"
                    :HID "HID"
                    :ICE "ICE"
                    :IMB "IMB"
                    :immediate-or-cancel "IOC"
                    :limit "LMT"
                    :limit-close "LMTCLS"
                    :limit-on-close "LOC"
                    :limit-on-open "LOO"
                    :limit-if-touched "LIT"
                    :LTH "LTH"
                    :market "MKT"
                    :market-close "MKTCLS"
                    :market-on-close "MOC"
                    :market-to-limit "MTL"
                    :market-with-protection "MKTPRT"
                    :market-if-touched "MIT"
                    :market-on-open "MOO"
                    :NONALGO "NONALGO"
                    :one-cancels-all "OCA"
                    :OPG "OPG"
                    :OPGREROUT "OPGREROUT"
                    :pegged-to-market "PEGMKT"
                    :pegged-to-midpoint "PEGMID"
                    :POSTONLY "POSTONLY"
                    :PREOPGRTH "PREOPGRTH"
                    :relative "REL"
                    :request-for-quote "QUOTE"
                    :RTH "RTH"
                    :RTHIGNOPG "RTHIGNOPG"
                    :scale "SCALE"
                    :SCALERST "SCALERST"
                    :stop "STP"
                    :stop-limit "STPLMT"
                    :SWEEP "SWEEP"
                    :TIMEPRIO "TIMEPRIO"
                    :trail "TRAIL"
                    :trail-limit "TRAILLIMIT"
                    :trailing-limit-if-touched "TRAILLIT"
                    :trailing-market-if-touched "TRAILMIT"
                    :trailing-stop "TRAIL"
                    :trailing-stop-limit "TRAILLMT"
                    :VWAP "VWAP"
                    :volatility "VOL"
                    :what-if "WHATIF"})

(translation-table order-status
                   {
                    :pending-submit "PendingSubmit"
                    :pending-cancel "PendingCancel"
                    :pre-submitted "PreSubmitted"
                    :submitted "Submitted"
                    :cancelled "Cancelled"
                    :filled "Filled"
                    :inactive "Inactive"
                    })

(translation-table security-id-type
                   {:isin "ISIN"
                    :cusip "CUSIP"
                    :sedol "SEDOL"
                    :ric "RIC"})

(translation-table tick-field-code
                   {
                    :bid-size 0
                    :bid-price 1
                    :ask-price 2
                    :ask-size 3
                    :last-price 4
                    :last-size 5
                    :high 6
                    :low 7
                    :volume 8
                    :close-price 9
                    :bid-option-computation 10
                    :ask-option-computation 11
                    :last-option-computation 12
                    :model-option-computation 13
                    :open-tick 14
                    :low-13-week 15
                    :high-13-week 16
                    :low-26-week 17
                    :high-26-week 18
                    :low-52-week 19
                    :high-52-week 20
                    :avg-volume 21
                    :open-interest 22
                    :option-historical-volatility 23
                    :option-implied-volatility 24
                    :option-bid-exchange 25
                    :option-ask-exchange 26
                    :option-call-open-interest 27
                    :option-put-open-interest 28
                    :option-call-volume 29
                    :option-put-volume 30
                    :index-future-premium 31
                    :bid-exchange 32
                    :ask-exchange 33
                    :auction-volume 34
                    :auction-price 35
                    :auction-imbalance 36
                    :mark-price 37
                    :bid-efp-computation 38
                    :ask-efp-computation 39
                    :last-efp-computation 40
                    :open-efp-computation 41
                    :high-efp-computation 42
                    :low-efp-computation 43
                    :close-efp-computation 44
                    :last-timestamp 45
                    :shortable 46
                    :fundamental-ratios 47
                    :realtime-volume 48
                    :halted 49
                    :bid-yield 50
                    :ask-yield 51
                    :last-yield 52
                    :cust-option-computation 53
                    :trade-count 54
                    :trade-rate 55
                    :volume-rate 56}
                   )

(translation-table generic-tick-type
                   {
                    :option-volume 100                   ; :option-call-volume, :option-put-volume
                    :option-open-interest 101            ; :option-call-open-interest, :option-put-open-interest
                    :historical-volatility 104           ; :option-historical-volatility
                    :option-implied-volatility 106       ; :option-implied-volatility
                    :index-future-premium 162            ; :index-future-premium
                    :miscellaneous-stats 165             ; :low-13-week, :high-13-week, :low-26-week, :high-26-week, :low-52-week, :high-52-week, :avg-volume 21
                    :mark-price 221                      ; :mark-price
                    :auction-values 225                  ; :auction-volume, :auction-price, :auction-imbalance
                    :realtime-volume 233                 ; :realtime-volume
                    :shortable 236                       ; :shortable
                    :inventory 256                       ;
                    :fundamental-ratios 258              ; :fundamental-ratios
                    :realtime-historical-volatility 411  ; 58?
                    })

(translation-table log-level
                   {:system 1
                    :error 2
                    :warning 3
                    :informational 4
                    :detail 5})

(defmethod translate [:to-ib :tick-list] [_ _ val]
  (->> val
       (map #(cond
              (valid? :to-ib :tick-field-code %)
              (translate :to-ib :tick-field-code %)

              (valid? :to-ib :generic-tick-type %)
              (translate :to-ib :generic-tick-type %)

              :else %))
       (map str)
       (clojure.string/join ",")))

(translation-table fundamental-ratio
                   {
                    :closing-price "NPRICE"
                    :3-year-ttm-growth "Three_Year_TTM_Growth"
                    :ttm/ttm "TTM_over_TTM"
                    :12-month-high "NHIG"
                    :12-month-low "NLOW"
                    :pricing-date "PDATE"
                    :10-day-average-volume "VOL10DAVG"
                    :market-cap "MKTCAP"
                    :eps-exclusing-extraordinary-items "TTMEPSXCLX"
                    :eps-normalized "AEPSNORM"
                    :revenue/share "TTMREVPS"
                    :common-equity-book-value/share "QBVPS"
                    :tangible-book-value/share "QTANBVPS"
                    :cash/share "QCSHPS"
                    :cash-flow/share "TTMCFSHR"
                    :dividends/share "TTMDIVSHR"
                    :dividend-rate "IAD"
                    :pe-excluding-extraordinary-items "PEEXCLXOR"
                    :pe-normalized "APENORM"
                    :price/sales "TMPR2REV"
                    :price/tangible-book "PR2TANBK"
                    :price/cash-flow/share "TTMPRCFPS"
                    :price/book "PRICE2BK"
                    :current-ration "QCURRATIO"
                    :quick-ratio "QQUICKRATI"
                    :long-term-debt/equity "QLTD2EQ"
                    :total-debt/equity "QTOTD2EQ"
                    :payout-ratio "TTMPAYRAT"
                    :revenue "TTMREV"
                    :ebita "TTMEBITD"
                    :ebt "TTMEBT"
                    :niac "TTMNIAC"
                    :ebt-normalized "AEBTNORM"
                    :niac-normalized "ANIACNORM"
                    :gross-margin "TTMGROSMGN"
                    :net-profit-margin "TTMNPMGN"
                    :operating-margin "TTMOPMGN"
                    :pretax-margin "APTMGNPCT"
                    :return-on-average-assets "TTMROAPCT"
                    :return-on-average-equity "TTMROEPCT"
                    :roi "TTMROIPCT"
                    :revenue-change "REVCHNGYR"
                    :revenue-change-ttm "TTMREVCHG"
                    :revenue-growth "REVTRENDGR"
                    :eps-change "EPSCHNGYR"
                    :eps-change-ttm "TTMEPSCHG"
                    :eps-growth "EPSTRENDGR"
                    :dividend-growth "DIVGRPCT"})

(translation-table account-value-key
                   {
                    :account-code "AccountCode"
                    :account-ready "AccountReady"
                    :account-type "AccountType"
                    :accrued-cash "AccruedCash"
                    :accrued-cash-commodities "AccruedCash-C"
                    :accrued-cash-stock "AccruedCash-S"
                    :accrued-dividend "AccruedDividend"
                    :accrued-dividend-commodities "AccruedDividend-C"
                    :accrued-dividend-stock "AccruedDividend-S"
                    :available-funds "AvailableFunds"
                    :available-funds-commodities "AvailableFunds-C"
                    :available-funds-stock "AvailableFunds-S"
                    :billable "Billable"
                    :billable-commodities "Billable-C"
                    :billable-stock "Billable-S"
                    :buying-power "BuyingPower"
                    :cash-balance "CashBalance"
                    :corporate-bond-value "CorporateBondValue"
                    :currency "Currency"
                    :cushion "Cushion"
                    :day-trades-remaining "DayTradesRemaining"
                    :day-trades-remaining-T+1 "DayTradesRemainingT+1"
                    :day-trades-remaining-T+2 "DayTradesRemainingT+2"
                    :day-trades-remaining-T+3 "DayTradesRemainingT+3"
                    :day-trades-remaining-T+4 "DayTradesRemainingT+4"
                    :equity-with-loan-value "EquityWithLoanValue"
                    :equity-with-loan-value-commodities "EquityWithLoanValue-C"
                    :equity-with-loan-value-stock "EquityWithLoanValue-S"
                    :excess-liquidity "ExcessLiquidity"
                    :excess-liquidity-commodities "ExcessLiquidity-C"
                    :excess-liquidity-stock "ExcessLiquidity-S"
                    :exchange-rate "ExchangeRate"
                    :full-available-funds "FullAvailableFunds"
                    :full-available-funds-commodities "FullAvailableFunds-C"
                    :full-available-funds-stock "FullAvailableFunds-S"
                    :full-excess-liquidity "FullExcessLiquidity"
                    :full-excess-liquidity-commodities "FullExcessLiquidity-C"
                    :full-excess-liquidity-stock "FullExcessLiquidity-S"
                    :full-initial-margin-requirement "FullInitMarginReq"
                    :full-initial-margin-requirement-commodities "FullInitMarginReq-C"
                    :full-initial-margin-requirement-stock "FullInitMarginReq-S"
                    :full-maintenance-margin-requirement "FullMaintMarginReq"
                    :full-maintenance-margin-requirement-commodities "FullMaintMarginReq-C"
                    :full-maintenance-margin-requirement-stock "FullMaintMarginReq-S"
                    :fund-value "FundValue"
                    :future-option-value "FutureOptionValue"
                    :futures-profit-loss "FuturesPNL"
                    :fx-cash-balance "FxCashBalance"
                    :gross-position-value "GrossPositionValue"
                    :net-liquidation "NetLiquidation"
                    :gross-position-value-stock "GrossPositionValue-S"
                    :indian-stock-haircut "IndianStockHaircut"
                    :indian-stock-haircut-commodities "IndianStockHaircut-C"
                    :indian-stock-haircut-stock "IndianStockHaircut-S"
                    :initial-margin-requirement "InitMarginReq"
                    :initial-margin-requirement-commodities "InitMarginReq-C"
                    :initial-margin-requirement-stock "InitMarginReq-S"
                    :leverage-stock "Leverage-S"
                    :look-ahead-available-funds "LookAheadAvailableFunds"
                    :look-ahead-available-funds-commodities "LookAheadAvailableFunds-C"
                    :look-ahead-available-funds-stock "LookAheadAvailableFunds-S"
                    :look-ahead-excess-liquidity "LookAheadExcessLiquidity"
                    :look-ahead-excess-liquidity-commodities "LookAheadExcessLiquidity-C"
                    :look-ahead-excess-liquidity-stock "LookAheadExcessLiquidity-S"
                    :look-ahead-initial-margin-requirement "LookAheadInitMarginReq"
                    :look-ahead-initial-margin-requirement-commodities "LookAheadInitMarginReq-C"
                    :look-ahead-initial-margin-requirement-stock "LookAheadInitMarginReq-S"
                    :look-ahead-maintenance-margin-requirement "LookAheadMaintMarginReq"
                    :look-ahead-maintenance-margin-requirement-commodities "LookAheadMaintMarginReq-C"
                    :look-ahead-maintenance-margin-requirement-stock "LookAheadMaintMarginReq-S"
                    :look-ahead-next-change "LookAheadNextChange"
                    :maintenance-margin-requirement "MaintMarginReq"
                    :maintenance-margin-requirement-commodities "MaintMarginReq-C"
                    :maintenance-margin-requirement-stock "MaintMarginReq-S"
                    :money-market-fund-value "MoneyMarketFundValue"
                    :mutual-fund-value "MutualFundValue"
                    :net-dividend "NetDividend"
                    :net-liquidation-commodities "NetLiquidation-C"
                    :net-liquidation-stock "NetLiquidation-S"
                    :net-liquidation-by-currency "NetLiquidationByCurrency"
                    :option-market-value "OptionMarketValue"
                    :pa-shares-value "PASharesValue"
                    :pa-shares-value-commodities "PASharesValue-C"
                    :pa-shares-value-stock "PASharesValue-S"
                    :post-expiration-excess "PostExpirationExcess"
                    :post-expiration-excess-commodities "PostExpirationExcess-C"
                    :post-expiration-excess-stock "PostExpirationExcess-S"
                    :post-expiration-margin "PostExpirationMargin"
                    :post-expiration-margin-commodities "PostExpirationMargin-C"
                    :post-expiration-margin-stock "PostExpirationMargin-S"
                    :profit-loss "PNL"
                    :previous-day-equity-with-loan-value "PreviousDayEquityWithLoanValue"
                    :previous-day-equity-with-loan-value-stock "PreviousDayEquityWithLoanValue-S"
                    :realized-profit-loss "RealizedPnL"
                    :regulation-T-equity "RegTEquity"
                    :regulation-T-equity-stock "RegTEquity-S"
                    :regulation-T-margin "RegTMargin"
                    :regulation-T-margin-stock "RegTMargin-S"
                    :sma "SMA"
                    :sma-stock "SMA-S"
                    :stock-market-value "StockMarketValue"
                    :t-bill-value "TBillValue"
                    :t-bond-value "TBondValue"
                    :total-cash-balance "TotalCashBalance"
                    :total-cash-value "TotalCashValue"
                    :total-cash-value-commodities "TotalCashValue-C"
                    :total-cash-value-stock "TotalCashValue-S"
                    :trading-type-stock "TradingType-S"
                    :unaltered-initial-margin-requirement "UnalteredInitMarginReq"
                    :unaltered-maintenance-margin-requirement "UnalteredMaintMarginReq"
                    :unrealized-profit-loss "UnrealizedPnL"
                    :warrants-value "WarrantValue"
                    :what-if-portfolio-margin-enabled "WhatIfPMEnabled"
                    })

(defn numeric-account-value? [key]
  (contains? #{:accrued-cash :accrued-cash-commodities :accrued-cash-stock
               :accrued-dividend :accrued-dividend-commodities :accrued-dividend-stock
               :available-funds :available-funds-commodities :available-funds-stock
               :billable :billable-commodities :billable-stock
               :buying-power :cash-balance :corporate-bond-value :cushion
               :equity-with-loan-value :equity-with-loan-value-commodities :equity-with-loan-value-stock
               :excess-liquidity :excess-liquidity-commodities :excess-liquidity-stock
               :exchange-rate
               :full-available-funds :full-available-funds-commodities :full-available-funds-stock
               :full-excess-liquidity :full-excess-liquidity-commodities :full-excess-liquidity-stock
               :full-initial-margin-requirement :full-initial-margin-requirement-commodities :full-initial-margin-requirement-stock
               :full-maintenance-margin-requirement :full-maintenance-margin-requirement-commodities :full-maintenance-margin-requirement-stock
               :fund-value :future-option-value :futures-profit-loss :fx-cash-balance
               :gross-position-value :gross-position-values-commodities :gross-position-value-stock
               :indian-stock-haricut :indian-stock-haircut-commodities :indian-stock-haircut-stock
               :initial-margin-requirement :initial-margin-requirement-commodities :initial-margin-requirement-stock
               :leverage :leverage-commodities :leverage-stock
               :look-ahead-available-funds :look-ahead-available-funds-commodities :look-ahead-available-funds-stock
               :look-ahead-excess-liquidity :look-ahead-excess-liquidity-commodities :look-ahead-excess-liquidity-stock
               :look-ahead-initial-margin-requirement :look-ahead-initial-margin-requirement-commodities :look-ahead-initial-margin-requirement-stock
               :look-ahead-maintenance-margin-requirement :look-ahead-maintenance-margin-requirement-commodities
               :look-ahead-maintenance-margin-requirement-stock
               :look-ahead-next-change
               :maintenance-margin-requirement :maintenance-margin-requirement-commodities :maintenance-margin-requirement-stock
               :money-market-fund-value :mutual-fund-value :net-dividend
               :net-liquidation :net-liquidation-commodities :net-liquidation-stock
               :net-liquidation-by-currency :option-market-value
               :pa-shares-value :pa-shares-value-commodities :pa-shares-value-stock
               :post-expiration-margin
               :post-expiration-margin-commodities :post-expiration-margin-stock
               :post-expiration-excess
               :post-expiration-excess-commodities :post-expiration-excess-stock
               :previous-day-equity-with-loan-value :previous-day-equity-with-loan-value-commodities :previous-day-equity-with-loan-value-stock
               :realized-profit-loss
               :regulation-T-equity :regulation-T-equity-commodities :regulation-T-equity-stock
               :regulation-T-margin :regulation-T-margin-commodities :regulation-T-margin-stock
               :sma :sma-commodities :sma-stock
               :stock-market-value :t-bill-value :t-bond-value :total-cash-balance
               :total-cash-value :total-cash-value-commodities :total-cash-value-stock
               :unaltered-initial-margin-requirement :unaltered-maintenance-margin-requirement
               :unrealized-profit-loss :warrants-value
               } key))

(defn integer-account-value? [key]
  (contains? #{:day-trades-remaining :day-trades-remaining-T+1 :day-trades-remaining-T+2
               :day-trades-remaining-T+3 :day-trades-remaining-T+4
               } key))

(defn boolean-account-value? [key]
  (contains? #{:account-ready :profit-loss :what-if-portfolio-margin-enabled} key))

(translation-table market-depth-row-operation
                   {
                    :insert 0
                    :update 1
                    :delete 2
                    })

(translation-table market-depth-side
                   {
                    :ask 0
                    :bid 1
                    })

(translation-table report-type
                   {
                    :estimates "Estimates"
                    :financial-statements "Financial Statements"
                    :summary "Summary"
                    })

(translation-table right
                   {:put "PUT"
                    :call "CALL"
                    :none "0"
                    :unknown "?"})

(translation-table rule-80A
                   {:individual "I"
                    :agency "A"
                    :agent-other-member "W"
                    :individual-PTIA "J"
                    :agency-PTIA "U"
                    :agent-other-member-PTIA "M"
                    :individual-PT "K"
                    :agency-PT "Y"
                    :agent-other-member-PT "N"})

(translation-table market-data-type
                   {:real-time-streaming 1
                    :frozen 2})

(translation-table boolean-int
                   {true 1
                    false 0})

(translation-table execution-side
                   {:buy "BOT"
                    :sell "SLD"})

(defmethod translate [:to-ib :duration] [_ _ [val unit]]
  (str val " " (translate :to-ib :duration-unit unit)))

(defmethod translate [:from-ib :duration] [_ _ val]
  (when val
    (let [[amount unit] (.split val " ")]
      (vector (Integer/parseInt amount)
              (translate :from-ib :duration-unit unit)))))

(defmethod translate [:from-ib :date-time] [_ _ val]
  (condp instance? val
    java.util.Date (tc/from-date val)
    String (translate :from-ib :date-time (Long/parseLong val))
    Long (tc/from-long (* 1000 val))))

(defmethod translate [:to-ib :date-time] [_ _ value]
  (when val
    (-> (tf/formatter "yyyyMMdd HH:mm:ss")
        (tf/unparse value)
        (str " UTC"))))

(defmethod translate [:to-ib :timestamp] [_ _ val]
  (condp instance? val
    java.util.Date (tc/from-date val)
    org.joda.time.DateTime (translate :to-ib :timestamp
                                      (-> (tf/formatter "yyyyMMdd-HH:mm:ss")
                                          (tf/unparse val)
                                          (str " UTC")))
    String val))

(defmethod translate [:from-ib :timestamp] [_ _ val]

  (cond
   (nil? val) nil

   (= (.length val) 8)
   (-> (tf/formatter "yyyyMMdd")
       (tf/parse val))

   (every? #(Character/isDigit %) val)
   (tc/from-long (* (Long/parseLong val) 1000))

   (= (.length val) 17)
   (-> (tf/formatter "yyyyMMdd-HH:mm:ss")
       (tf/parse val))

   :otherwise val))

(defmethod translate [:from-ib :time-zone] [_ _ val]
  (case val
    "GMT" "+0000"
    "EST" "-0500"
    "MST" "-0700"
    "PST" "-0800"
    "AST" "-0400"
    "JST" "+0900"
    "AET" "+1000"))

(defmethod translate [:from-ib :connection-time] [_ _ val]
  (when val
    (let [tokens (vec (.split val " "))
          timezone-token (get tokens 2)]
      (when timezone-token
        (let [timezone-offset (translate :from-ib :time-zone timezone-token)
              tokens-with-adjusted-timezone (concat (take 2 tokens) [timezone-offset])
              adjusted-date-time-string (apply str (interpose " " tokens-with-adjusted-timezone))]
          (-> (tf/formatter "yyyyMMdd HH:mm:ss Z")
              (tf/parse adjusted-date-time-string)))))))

(defmethod translate [:to-ib :connection-time] [_ _ val]
  (when val
    (-> (tf/formatter "yyyyMMdd HH:mm:ss z")
        (tf/unparse val))))

(defmethod translate [:to-ib :date] [_ _ val]
  (tf/unparse (tf/formatter "MM/dd/yyyy") val))

(defmethod translate [:from-ib :date] [_ _ val]
  (when val
    (try
      (tf/parse (tf/formatter "MM/dd/yyyy") val)
      (catch Exception e
        (throw (ex-info "Failed to translate from IB date value."
                        {:value val
                         :expected-form "MM/dd/yyyy"}))))))

;;; FIXME: We should turn time of day into some kind of data structure that does
;;; no have a date component.
(defmethod translate [:from-ib :time-of-day] [_ _ val]
  (when val
    (try
      (tf/parse (tf/formatter "HH:mm") val)
      (catch Exception e
        (throw (ex-info "Failed to translate from IB time-of-day value."
                        {:value val
                         :expected-form "HH:mm"}))))))

(defmethod translate [:to-ib :time-of-day] [_ _ val]
  (when val
    (try
      (tf/unparse (tf/formatter "HH:mm") val)
      (catch Exception e
        (throw (ex-info "Failed to translate from IB time-of-day value."
                        {:value val
                         :expected-form "HH:mm"}))))))

(defmethod translate [:to-ib :expiry] [_ _ val]
  (when val
    (let [y (time/year val)
          ys (.toString y)
          m (time/month val)
          ms (format "%02d" m)]
      (str ys ms))))

(defmethod translate [:from-ib :expiry] [_ _ val]
  (condp = (.length val)
    6 (tf/parse (tf/formatter "yyyyMM") val)
    8 (tf/parse (tf/formatter "yyyyMMdd") val)))

(defmethod translate [:to-ib :bar-size] [_ _ [val unit]]
  (str val " " (translate :to-ib :bar-size-unit unit)))

(defmethod translate [:to-ib :double-string] [_ _ val]
  (str val))

(defmethod translate [:from-ib :double-string] [_ _ val]
  (Double/parseDouble val))

(defmethod translate [:from-ib :order-types] [_ _ val]
  (->> (.split val ",")
       (map (partial translate :from-ib :order-type))))

(defmethod translate [:to-ib :order-types] [_ _ val]
  (str/join "," (map (partial translate :to-ib :order-type) val)))

(defmethod translate [:from-ib :exchanges] [_ _ val]
  (str/split val #","))

(defmethod translate [:to-ib :exchanges] [_ _ val]
  (str/join "," val))

(defmethod translate [:from-ib :yield-redemption-date] [_ _ val]
  (let [year (int (Math/floor (/ val 10000)))
        month (int (Math/floor (/ (mod val 10000) 100)))
        day (int (Math/floor (mod 19720427 100)))]
    (time/date-time year month day)))

;; -----
;; ## Deals with the trading hours reporting.  This is really ugly.
;; IB uses their timezone definitions incorrectly.  Correct them here.  No, no, they really do.
(def ib-timezone-map
  {"EST" "America/New_York"
   "CST" "America/Chicago"
   "JST" "Asia/Tokyo"})

(defn- to-utc
  "Returns a full date-time in UTC, referring to a particular time at a particular place. Place must be a TZ string such as America/Chicago. Date will only use the year-month-day fields, the min and second come from the parms."
  ([place date-time]
     (to-utc place date-time (time/hour date-time) (time/minute date-time) (time/sec date-time)))
  ([place date hour minute]
     (to-utc place date hour minute 0))
  ([place date hour minute second]
     (let [zone (time/time-zone-for-id (or (ib-timezone-map place) place))]
       (time/to-time-zone
        (time/from-time-zone
         (time/date-time (time/year date) (time/month date) (time/day date) hour minute second)
         zone)
        (time/time-zone-for-id "UTC")))))

(defn- th-days
  "Returns a seq of the days in an interval"
  [s]
  (str/split s #";"))

(defn- th-components [s]
  (str/split s #":"))

;; NB: Closed days are represented as 0 length intervals on that day.
(defn- th-intervals [[day s]]
  (if (= s "CLOSED")
    [[(str day "0000") (str day "0000")]]
    (map #(mapv (partial str day) (str/split % #"-")) (str/split s #","))))

;; Convert to Joda intervals
(defn- joda-interval [tz [start end]]
  [start end]
  (let [start-dt  (to-utc tz (tf/parse (tf/formatter "yyyyMMddHHmm") start))
        end-dt    (to-utc tz (tf/parse (tf/formatter "yyyyMMddHHmm") end))
        mod-start (if (time/after? start-dt end-dt)
                    (time/minus start-dt (time/days 1))
                    start-dt)]
    (time/interval mod-start end-dt)))

(defmethod translate [:from-ib :trading-hours] [_ _ [tz t-string]]
  (->> t-string
       th-days
       (map th-components)
       (mapcat th-intervals)
       (map (partial joda-interval tz))))
