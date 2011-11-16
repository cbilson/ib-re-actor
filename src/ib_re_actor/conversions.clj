(ns ib-re-actor.conversions
  (:use [clojure.contrib.def :only [defmacro-]]
        [clj-time.core :only [year month day date-time
                              plus minus years months weeks days hours minutes secs millis
                              from-time-zone to-time-zone time-zone-for-id]]
        [clj-time.format :only [formatter formatters parse unparse]]
        [clj-time.coerce :only [from-long to-long]]))

(defmacro- translation-table [name vals]
  `(let [vals# ~vals]
     (defn ~(symbol (str "translate-to-ib-" name)) [val#]
       (get vals# val#))
     (defn ~(symbol (str "translate-from-ib-" name)) [val#]
       (get (zipmap (vals vals#)
                    (keys vals#))
            val#))))

;; example
#_ (translation-table size-tick-field-code
                      {:bid-size 0
                       :ask-size 3
                       :last-size 5
                       :volume 8})

(translation-table duration-unit
                   {
                    :second "S"
                    :seconds "S"
                    :day "D"
                    :days "D"
                    :week "W"
                    :weeks "W"
                    :year "Y"
                    :years "Y"
                    })

(translation-table security-type
                   {
                    :equity "STK"
                    :option "OPT"
                    :future "FUT"
                    :index "IND"
                    :future-option "FOP"
                    :cash "CASH"
                    :bag "BAG"
                    })

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
                    :trailing-stop-limit "TRAILLIMIT"
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

(translation-table tick-type
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
                    :cash-balance "CashBalance"
                    :day-trades-remaining "DayTradesRemaining"
                    :equity-with-loan-value "EquityWithLoanValue"
                    :initial-margin-required "InitMarginReq"
                    :maintenance-margin-required "MaintMarginReq"
                    :net-liquidation "NetLiquidation"
                    })

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

(defn translate-to-ib-duration [val unit]
  (str val " " (translate-to-ib-duration-unit unit)))

(defn translate-from-ib-date-time [val]
  (condp instance? val
    String (translate-from-ib-date-time (Long/parseLong val))
    Long (from-long (* 1000 val))))

(defn translate-to-ib-expiry [val]
  (let [y (year val)
        ys (.toString y)
        m (month val)
        ms (format "%02d" m)]
    (str ys ms)))

(defn translate-from-ib-expiry [val]
  (condp = (.length val)
    6 (parse (formatter "yyyyMM") val)
    8 (parse (formatter "yyyyMMdd") val)))

(defn translate-to-ib-date-time [value]
  (-> (formatter "yyyyMMdd HH:mm:ss")
      (unparse value)
      (str " UTC")))

(defn translate-to-ib-bar-size [val unit]
  (str val " " (translate-to-ib-bar-size-unit unit)))

(defn translate-from-ib-contract [c]
  {:symbol (.m_symbol c)
   :security-type (.m_secType c)
   :expiry (translate-from-ib-expiry (.m_expiry c))
   :strike (.m_strike c)
   :right (.m_right c)
   :multiplier (.m_multiplier c)
   :exchange (.m_exchange c)
   :currency (.m_currency c)
   :local-symbol (.m_localSymbol c)
   :primary-exchange (.m_primaryExch c)
   :include-expired? (.m_includeExpired c)
   :combo-legs-description (.m_comboLegsDescrip c)
   :combo-legs (.m_comboLegs c)
   :contract-id (.m_conId c)
   :security-id-type (.m_secIdType c)
   :security-id (.m_secId c)
   })

(defn translate-to-ib-contract [c]
  (doto (com.ib.client.Contract.)
    (.m_symbol (:symbol c))
    (.m_secType (translate-to-ib-security-type (:security-type c)))
    (.m_expiry (translate-to-ib-expiry (:expiry c)))
    (.m_strike (:strike c))
    (.m_right (:right c))
    (.m_multiplier (:multiplier c))
    (.m_exchange (:exchange c))
    (.m_currency (:currency c))
    (.m_localSymbol (:local-symbol c))
    (.m_primaryExch (:primary-exchange c))
    (.m_includeExpired (:include-expired? c))
    (.m_comboLegsDescrip (:combo-legs-description c))
    (.m_comboLegs (:combo-legs c))
    (.m_conId (:contract-id c))
    (.m_secIdType (:security-id-type c))
    (.m_secId (:security-id c))))

(defn translate-from-ib-contract-details [c]
  {:contract (translate-from-ib-contract (.m_summary c))
   :market-name (.m_marketName c)
   :trading-class (.m_tradingClass c)
   :minimum-prices-tick (.m_minTick c)
   :price-magnifier (.m_priceMagnifier c)
   :order-types (vec (map translate-from-ib-order-type (.split (.m_orderTypes c) ",")))
   :valid-exchanges (vec (.split (.m_validExchanges c) ","))
   :underlying-contract-id (.m_underConId c)
   :long-name (.m_longName c)
   :CUSIP (.m_cusip c)
   :ratings (.m_ratings c)
   :description (.m_descAppend c)
   :bond-type (.m_bondType c)
   :coupon-type (.m_couponType c)
   :callable? (.m_callable c)
   :putable? (.m_putable c)
   :coupon (.m_coupon c)
   :convertible (.m_convertible c)
   :maturity (.m_maturity c)
   :issue-date (.m_issueDate c)
   :next-option-date (.m_nextOptionDate c)
   :next-option-type (.m_nextOptionType c)
   :next-option-partial (.m_nextOptionPartial c)
   :notes (.m_notes c)
   :contract-month (.m_contractMonth c)
   :industry (.m_industry c)
   :category (.m_category c)
   :subcategory (.m_subcategory c)
   :time-zone-id (.m_timeZoneId c)
   :trading-hours (vec (.split (.m_tradingHours c) ";"))
   :liquid-hours (vec (.split (.m_liquidHours c) ";"))
   })