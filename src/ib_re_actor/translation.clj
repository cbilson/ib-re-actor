(ns ib-re-actor.translation
  (:require [clj-time.core :as time]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [clojure.string :as str]))

(defmulti ^:dynamic translate (fn [direction table-name _] [direction table-name]))
(defmulti ^:dynamic valid? (fn [direction table-name _] [direction table-name]))

(defmacro translation-table [name vals]
  (let [table-name (keyword name)]
    `(let [to-table# ~vals
           from-table# (zipmap (vals to-table#) (keys to-table#))]

       (def ~name to-table#)

       (defmethod valid? [:to-ib ~table-name] [_# _# val#]
         (contains? to-table# val#))

       (defmethod translate [:to-ib ~table-name] [_# _# val#]
         (if val#
           (if (valid? :to-ib ~table-name val#)
             (to-table# val#)
             (throw (ex-info (str "Can't translate to IB " ~table-name " " val#)
                             {:value val#
                              :table ~table-name
                              :valid-values (keys to-table#)})))))

       (defmethod valid? [:from-ib ~table-name] [_# _# val#]
         (contains? from-table# val#))

       (defmethod translate [:from-ib ~(keyword name)] [_# _# val#]
         (if val#
           (if (valid? :from-ib ~table-name val#)
             (from-table# val#)
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

(translation-table security-type
                   {:equity "STK"
                    :option "OPT"
                    :future "FUT"
                    :index "IND"
                    :future-option "FOP"
                    :cash "CASH"
                    :bag "BAG"})

(translation-table bar-size-unit
                   {:second "sec"
                    :seconds "secs"
                    :minute "min"
                    :minutes "mins"
                    :hour "hour"
                    :hours "hour"
                    :day "day"
                    :days "days"})

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
                    :ALLOC "ALLOC"
                    :average-cost "AVGCOST"
                    :basket "BASKET"
                    :COND "COND"
                    :CONDORDER "CONDORDER"
                    :DAY "DAY"
                    :DEACT "DEACT"
                    :DEACTDIS "DEACTDIS"
                    :DEACTEOD "DEACTEOD"
                    :box-top "BOXTOP"
                    :good-after-time "GAT"
                    :good-till-date "GTD"
                    :good-till-canceled "GTC"
                    :GTT "GTT"
                    :HID "HID"
                    :ICE "ICE"
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
                    :pegged-to-market "PEGMKT"
                    :pegged-to-midpoint "PEGMID"
                    :relative "REL"
                    :request-for-quote "QUOTE"
                    :scale "SCALE"
                    :SCALERST "SCALERST"
                    :stop "STP"
                    :stop-limit "STPLMT"
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
                    :accrued-cash-c "AccruedCash-C"
                    :accrued-cash-s "AccruedCash-S"
                    :accrued-dividend "AccruedDividend"
                    :accrued-dividend-c "AccruedDividend-C"
                    :accrued-dividend-s "AccruedDividend-S"
                    :available-funds "AvailableFunds"
                    :available-funds-c "AvailableFunds-C"
                    :available-funds-s "AvailableFunds-S"
                    :billable "Billable"
                    :billable-c "Billable-C"
                    :billable-s "Billable-S"
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
                    :equity-with-loan-value-c "EquityWithLoanValue-C"
                    :equity-with-loan-value-s "EquityWithLoanValue-S"
                    :excess-liquidity "ExcessLiquidity"
                    :excess-liquidity-c "ExcessLiquidity-C"
                    :excess-liquidity-s "ExcessLiquidity-S"
                    :exchange-rate "ExchangeRate"
                    :full-available-funds "FullAvailableFunds"
                    :full-available-funds-C "FullAvailableFunds-C"
                    :full-available-funds-S "FullAvailableFunds-S"
                    :full-excess-liquidity "FullExcessLiquidity"
                    :full-excess-liquidity-C "FullExcessLiquidity-C"
                    :full-excess-liquidity-S "FullExcessLiquidity-S"
                    :full-initial-margin-requirement "FullInitMarginReq"
                    :full-initial-margin-requirement-C "FullInitMarginReq-C"
                    :full-initial-margin-requirement-S "FullInitMarginReq-S"
                    :full-maintenance-margin-requirement "FullMaintMarginReq"
                    :full-maintenance-margin-requirement-C "FullMaintMarginReq-C"
                    :full-maintenance-margin-requirement-S "FullMaintMarginReq-S"
                    :fund-value "FundValue"
                    :future-option-value "FutureOptionValue"
                    :futures-profit-loss "FuturesPNL"
                    :fx-cash-balance "FxCashBalance"
                    :gross-position-value "GrossPositionValue"
                    :net-liquidation "NetLiquidation"
                    :gross-position-value-S "GrossPositionValue-S"
                    :indian-stock-haircut "IndianStockHaircut"
                    :indian-stock-haircut-C "IndianStockHaircut-C"
                    :indian-stock-haircut-S "IndianStockHaircut-S"
                    :initial-margin-requirement "InitMarginReq"
                    :initial-margin-requirement-C "InitMarginReq-C"
                    :initial-margin-requirement-S "InitMarginReq-S"
                    :leverage-S "Leverage-S"
                    :look-ahead-available-funds "LookAheadAvailableFunds"
                    :look-ahead-available-funds-C "LookAheadAvailableFunds-C"
                    :look-ahead-available-funds-S "LookAheadAvailableFunds-S"
                    :look-ahead-excess-liquidity "LookAheadExcessLiquidity"
                    :look-ahead-excess-liquidity-C "LookAheadExcessLiquidity-C"
                    :look-ahead-excess-liquidity-S "LookAheadExcessLiquidity-S"
                    :look-ahead-initial-margin-requirement "LookAheadInitMarginReq"
                    :look-ahead-initial-margin-requirement-C "LookAheadInitMarginReq-C"
                    :look-ahead-initial-margin-requirement-S "LookAheadInitMarginReq-S"
                    :look-ahead-maintenance-margin-requirement "LookAheadMaintMarginReq"
                    :look-ahead-maintenance-margin-requirement-C "LookAheadMaintMarginReq-C"
                    :look-ahead-maintenance-margin-requirement-S "LookAheadMaintMarginReq-S"
                    :look-ahead-next-change "LookAheadNextChange"
                    :maintenance-margin-requirement "MaintMarginReq"
                    :maintenance-margin-requirement-C "MaintMarginReq-C"
                    :maintenance-margin-requirement-S "MaintMarginReq-S"
                    :money-market-fund-value "MoneyMarketFundValue"
                    :mutual-fund-value "MutualFundValue"
                    :net-dividend "NetDividend"
                    :net-liquidation-C "NetLiquidation-C"
                    :net-liquidation-S "NetLiquidation-S"
                    :net-liquidation-by-currency "NetLiquidationByCurrency"
                    :option-market-value "OptionMarketValue"
                    :pa-shares-value "PASharesValue"
                    :pa-shares-value-C "PASharesValue-C"
                    :pa-shares-value-S "PASharesValue-S"
                    :profit-loss "PNL"
                    :previous-day-equity-with-loan-value "PreviousDayEquityWithLoanValue"
                    :previous-day-equity-with-loan-value-S "PreviousDayEquityWithLoanValue-S"
                    :realized-profit-loss "RealizedPnL"
                    :regulation-T-equity "RegTEquity"
                    :regulation-T-equity-S "RegTEquity-S"
                    :regulation-T-margin "RegTMargin"
                    :regulation-T-margin-S "RegTMargin-S"
                    :sma "SMA"
                    :sma-s "SMA-S"
                    :stock-market-value "StockMarketValue"
                    :t-bill-value "TBillValue"
                    :t-bond-value "TBondValue"
                    :total-cash-balance "TotalCashBalance"
                    :total-cash-value "TotalCashValue"
                    :total-cash-value-C "TotalCashValue-C"
                    :total-cash-value-S "TotalCashValue-S"
                    :trading-type-s "TradingType-S"
                    :unaltered-initial-margin-requirement "UnalteredInitMarginReq"
                    :unaltered-maintenance-margin-requirement "UnalteredMaintMarginReq"
                    :unrealized-profit-loss "UnrealizedPnL"
                    :warrants-value "WarrantValue"
                    :what-if-portfolio-margin-enabled "WhatIfPMEnabled"
                    })

(defn numeric-account-value? [key]
  (contains? #{:accrued-cash :accrued-cash-c :accrued-cash-s
               :accrued-dividend :accrued-dividend-c :accrued-dividend-s
               :available-funds :available-funds-c :available-funds-s
               :billable :billable-c :billable-s
               :buying-power :cash-balance :corporate-bond-value :cushion
               :equity-with-loan-value :equity-with-loan-value-c :equity-with-loan-value-s
               :excess-liquidity :excess-liquidity-c :excess-liquidity-s
               :exchange-rate
               :full-available-funds :full-available-funds-c :full-available-funds-s
               :full-excess-liquidity :full-excess-liquidity-c :full-excess-liquidity-s
               :full-initial-margin-requirement :full-initial-margin-requirement-c :full-initial-margin-requirement-s
               :full-maintenance-margin-requirement :full-maintenance-margin-requirement-c :full-maintenance-margin-requirement-s
               :fund-value :future-option-value :futures-profit-loss :fx-cash-balance
               :gross-position-value :gross-position-values-c :gross-position-value-s
               :indian-stock-haricut :indian-stock-haricut-c :indian-stock-haricut-s
               :initial-margin-requirement :initial-margin-requirement-c :initial-margin-requirement-s
               :leverage :leverage-c :leverage-s
               :look-ahead-available-funds :look-ahead-available-funds-c :look-ahead-available-funds-s
               :look-ahead-excess-liquidity :look-ahead-excess-liquidity-c :look-ahead-excess-liquidity-s
               :look-ahead-initial-margin-requirement :look-ahead-initial-margin-requirement-c :look-ahead-initial-margin-requirement-s
               :look-ahead-maintenance-margin-requirement :look-ahead-maintenance-margin-requirement-c :look-ahead-maintenance-margin-requirement-s
               :look-ahead-next-change
               :maintenance-margin-required :maintenance-margin-required-c :maintenance-margin-required-s
               :money-market-fund-value :mutual-fund-value :net-dividend
               :net-liquidation :net-liquidation-c :net-liquidation-s
               :net-liquidation-by-currency :option-market-value
               :pa-shares-value :pa-shares-value-c :pa-shares-value-s
               :previous-day-equity-with-loan-value :previous-day-equity-with-loan-value-c :previous-day-equity-with-loan-value-s
               :realized-profit-loss
               :regulation-T-equity :regulation-T-equity-c :regulation-T-equity-s
               :regulation-T-margin :regulation-T-margin-c :regulation-T-margin-s
               :sma :sma-c :sma-s
               :stock-market-value :t-bill-value :t-bond-value :total-cash-balance
               :total-cash-value :total-cash-value-c :total-cash-value-s
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
                    :call "CALL"})

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

(translation-table boolean-int
                   {true 1
                    false 0})

(defmethod translate [:to-ib :duration] [_ _ [val unit]]
  (str val " " (translate :to-ib :duration-unit unit)))

(defmethod translate [:from-ib :duration] [_ _ val]
  (if val
    (let [[amount unit] (.split val " ")]
      (vector (Integer/parseInt amount)
              (translate :from-ib :duration-unit unit)))))

(defmethod translate [:from-ib :date-time] [_ _ val]
  (condp instance? val
    java.util.Date (tc/from-date val)
    String (translate :from-ib :date-time (Long/parseLong val))
    Long (tc/from-long (* 1000 val))))

(defmethod translate [:to-ib :date-time] [_ _ value]
  (if val
    (-> (tf/formatter "yyyyMMdd HH:mm:ss")
        (tf/unparse value)
        (str " UTC"))))

(defmethod translate [:to-ib :timestamp] [_ _ val]
  (condp instance? val
    java.util.Date (tc/from-date val)
    org.joda.time.DateTime (translate :to-ib :timestamp
                                      (-> (tf/formatter "yyyyMMdd-hh:mm:ss")
                                          (tf/unparse val)
                                          (str " UTC")))
    String val))

(defmethod translate [:from-ib :timestamp] [_ _ val]
  (if val
    (-> (tf/formatter "yyyyMMdd-hh:mm:ss")
        (tf/parse val))))

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
  (if val
    (let [tokens (vec (.split val " "))
          timezone-token (get tokens 2)]
      (if timezone-token
        (let [timezone-offset (translate :from-ib :time-zone timezone-token)
              tokens-with-adjusted-timezone (concat (take 2 tokens) [timezone-offset])
              adjusted-date-time-string (apply str (interpose " " tokens-with-adjusted-timezone))]
          (-> (tf/formatter "yyyyMMdd HH:mm:ss Z")
              (tf/parse adjusted-date-time-string)))))))

(defmethod translate [:to-ib :connection-time] [_ _ val]
  (if val
    (-> (tf/formatter "yyyyMMdd HH:mm:ss z")
        (tf/unparse val))))

(defmethod translate [:to-ib :date] [_ _ val]
  (tf/unparse (tf/formatter "MM/dd/yyyy") val))

(defmethod translate [:from-ib :date] [_ _ val]
  (if val
    (try
      (tf/parse (tf/formatter "MM/dd/yyyy") val)
      (catch Exception e
        (throw (ex-info "Failed to translate from IB date value."
                        {:value val
                         :expected-form "MM/dd/yyyy"}))))))

(defmethod translate [:from-ib :time-of-day] [_ _ val]
  (if val
    (try
      (tf/parse (tf/formatter "HH:mm") val)
      (catch Exception e
        (throw (ex-info "Failed to translate from IB time-of-day value."
                        {:value val
                         :expected-form "HH:mm"}))))))

(defmethod translate [:to-ib :time-of-day] [_ _ val]
  (if val
    (try
      (tf/unparse (tf/formatter "HH:mm") val)
      (catch Exception e
        (throw (ex-info "Failed to translate from IB time-of-day value."
                        {:value val
                         :expected-form "HH:mm"}))))))
(defmethod translate [:to-ib :expiry] [_ _ val]
  (let [y (time/year val)
        ys (.toString y)
        m (time/month val)
        ms (format "%02d" m)]
    (str ys ms)))

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
  (str/join "," (map name val)))
