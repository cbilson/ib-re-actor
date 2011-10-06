(ns re-actor.conversions
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
                    :futures "FUT"
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
                   {
                    :limit "LMT"
                    :limit-close "LMTCLS"
                    :market "MKT"
                    :market-close "MKTCLS"
                    :pegged-to-market "PEGMKT"
                    :rel "REL"
                    :scale "SCALE"
                    :stop "STP"
                    :stop-limit "STPLMT"
                    :trail "TRAIL"
                    :trail-limit "TRAILLIMIT"
                    :vwap "VWAP"
                    })

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
                    :option-historical-vol 23
                    :option-implied-vol 24
                    :option-bid-exch 25
                    :option-ask-exch 26
                    :option-call-open-interest 27
                    :option-put-open-interest 28
                    :option-call-volume 29
                    :option-put-volume 30
                    :index-future-premium 31
                    :bid-exch 32
                    :ask-exch 33
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
                    :rt-volume 48
                    :halted 49
                    :bid-yield 50
                    :ask-yield 51
                    :last-yield 52
                    :cust-option-computation 53
                    :trade-count 54
                    :trade-rate 55
                    :volume-rate 56}
                   )

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
  (parse (formatter "yyyyMM") val))

(defn translate-to-ib-date-time [value]
  (-> (formatter "yyyy MM dd HH:mm:ss")
      (unparse value)))

(defn translate-to-ib-bar-size [val unit]
  (str val " " (translate-to-ib-bar-size-unit unit)))
