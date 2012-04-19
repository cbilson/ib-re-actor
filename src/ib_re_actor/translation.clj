(ns ib-re-actor.translation
  (:require [clj-time.core :as time]
            [clj-time.format :as tf]
            [clj-time.coerce :as tc]
            [clojure.string :as str]))

(defmulti ^:dynamic translate (fn [direction table-name _] [direction table-name]))
(defmulti ^:dynamic validate (fn [direction table-name _] [direction table-name]))

(defmacro translation-table [name vals]
  (let [table-name (keyword name)]
    `(let [to-table# ~vals
           from-table# (zipmap (vals to-table#) (keys to-table#))]

       (defmethod validate [:to-ib ~table-name] [_# _# val#]
         (contains? to-table# val#))

       (defmethod translate [:to-ib ~table-name] [_# _# val#]
         (if val#
           (if (validate :to-ib ~table-name val#)
             (to-table# val#)
             (throw (ex-info (str "Can't translate to IB " ~table-name " " val#)
                             {:value val#
                              :table ~table-name
                              :valid-values (keys to-table#)})))))

       (defmethod validate [:from-ib ~table-name] [_# _# val#]
         (contains? from-table# val#))

       (defmethod translate [:from-ib ~(keyword name)] [_# _# val#]
         (if val#
           (if (validate :from-ib ~table-name val#)
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

(defmethod translate [:to-ib :date] [_ _ val]
  (tf/unparse (tf/formatter "MM/dd/yyyy") val))

(defmethod translate [:from-ib :date] [_ _ val]
  (if val
    (try
      (tf/parse (tf/formatter "MM/dd/yyyy") val)
      (catch Exception e
        (throw (ex-info "Failed to translate from IB date value."
                        {:value val
                         :expected-forme "MM/dd/yyyy"}))))))

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