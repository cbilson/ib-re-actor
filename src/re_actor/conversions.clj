(ns re-actor.conversions
  (:use [clj-time.core :only [year month day date-time
                              plus minus years months weeks days hours minutes secs millis
                              from-time-zone to-time-zone time-zone-for-id]]
        [clj-time.format :only [formatter formatters parse unparse]]
        [clj-time.coerce :only [from-long to-long]]))

(defn translate-to-ib-duration
  ([duration unit]
     (str duration " " 
          (condp = unit
            :second "S"
            :seconds "S"
            :day "D"
            :days "D"
            :week "W"
            :weeks "W"
            :year "Y"
            :years "Y"))))

(defn translate-to-ib-security-type [type]
  (condp = type
    :stock "STK"
    :equity "STK"
    :option "OPT"
    :future "FUT"
    :futures "FUT"
    :index "IND"
    :future-option "FOP"
    :cash "CASH"
    :bag "BAG"))

(defn translate-from-ib-tick-field [field-id]
  (condp = field-id
    1 :bid
    2 :ask
    4 :last
    6 :high
    7 :low
    9 :close))

(defn translate-bar-size [amount unit]
  (str amount " "
       (condp = unit
         :second "sec"
         :seconds "secs"
         :minute "min"
         :minutes "mins"
         :hour "hour"
         :hours "hour"
         :day "day"
         :days "days")))

(defn translate-what-to-show [what-to-show]
  (condp = what-to-show
    :trades "TRADES"
    :midpoint "MIDPOINT"
    :bid "BID"
    :ask "ASK"
    :bid-ask "BID_ASK"
    :historical-volatility "HISTORICAL_VOLATILITY"
    :option-implied-volatility "OPTION_IMPLIED_VOLATILITY"
    :option-volume "OPTION_VOLUME"
    :option-open-interest "OPTION_OPEN_INTEREST"))

(defn translate-from-ib-date-time [val]
  (condp instance? val
    String (translate-from-ib-date-time (Long/parseLong val))
    Long (from-long (* 1000 val))))

(defn translate-to-ib-expiry [expiry-date]
  (let [y (year expiry-date)
        ys (.toString y)
        m (month expiry-date)
        ms (format "%02d" m)]
    (str ys ms)))

(defn translate-time-in-force [value]
  (condp = value
    :day "DAY"
    :good-to-close "GTC"
    :immediate-or-cancel "IOC"
    :good-till-date "GTD"))

(defn translate-to-ib-date-time [value]
  (-> (formatter "yyyy MM dd HH:mm:ss")
      (unparse value)))

(defn translate-to-ib-order-action [action]
  (condp = action
    :buy "BUY"
    :sell "SELL"
    :sell-short "SSHORT"))
