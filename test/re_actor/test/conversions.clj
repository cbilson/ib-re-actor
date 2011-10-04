(ns re-actor.test.conversions
  (:use [re-actor.conversions]
        [clj-time.core :only [date-time]]
        [midje.sweet]))

(tabular
 (fact "it can translate to IB durations"
   (translate-to-ib-duration ?value ?unit) => ?expected)
 :where
 | ?value | ?unit    | ?expected |
 | 1      | :second  | "1 S"     |
 | 5      | :seconds | "5 S"     |
 | 1      | :day     | "1 D"     |
 | 5      | :days    | "5 D"     |
 | 1      | :week    | "1 W"     |
 | 5      | :weeks   | "5 W"     |
 | 1      | :year    | "1 Y"     |
 | 5      | :years   | "5 Y"     |)

(tabular
 (fact "it can translate to IB security codes"
   (translate-to-ib-security-type ?value) => ?expected)
 :where
 | ?value            | ?expected |
 | :stock            | "STK"     |
 | :equity           | "STK"     |
 | :option           | "OPT"     |
 | :future           | "FUT"     |
 | :futures          | "FUT"     |
 | :index            | "IND"     |
 | :future-option    | "FOP"     |
 | :cash             | "CASH"    |
 | :bag              | "BAG"     |)

(tabular
 (fact "it can translate from IB tick field codes"
   (translate-from-ib-tick-field ?field) => ?expected)
 :where
 | ?field | ?expected |
 | 1      | :bid      |
 | 2      | :ask      |
 | 4      | :last     |
 | 6      | :high     |
 | 7      | :low      |
 | 9      | :close    |)

(tabular
 (fact "it can translate bar sizes"
   (translate-bar-size ?value ?unit) => ?expected)
 :where
 | ?value | ?unit    | ?expected |
 | 1      | :second  | "1 sec"   |
 | 5      | :seconds | "5 secs"  |
 | 1      | :minute  | "1 min"   |
 | 3      | :minutes | "3 mins"  |
 | 1      | :hour    | "1 hour"  |
 | 4      | :hours   | "4 hour"  |
 | 1      | :day     | "1 day"   |
 | 2      | :days    | "2 days"  |)

(tabular
 (fact "it can translate what to show strings"
   (translate-what-to-show ?value) => ?expected)
 :where
 | ?value                     | ?expected                   |
 | :trades                    | "TRADES"                    |
 | :midpoint                  | "MIDPOINT"                  |
 | :bid                       | "BID"                       |
 | :ask                       | "ASK"                       |
 | :bid-ask                   | "BID_ASK"                   |
 | :historical-volatility     | "HISTORICAL_VOLATILITY"     |
 | :option-implied-volatility | "OPTION_IMPLIED_VOLATILITY" |
 | :option-volume             | "OPTION_VOLUME"             |
 | :option-open-interest      | "OPTION_OPEN_INTEREST"      |)

(fact "it can translate from IB date-time values"
  (translate-from-ib-date-time (long 1000000000)) => (date-time 2001 9 9 1 46 40)
  (translate-from-ib-date-time "1000000000") => (date-time 2001 9 9 1 46 40))

(fact "it can translate date-times to IB expiry strings"
  (translate-to-ib-expiry (date-time 2011 9 17)) => "201109")

(fact "it can translate from IB expiry strings to joda DateTimes"
  (translate-from-ib-expiry "201109") => (date-time 2011 9))

(tabular 
 (fact "it can translate time in force values"
   (translate-time-in-force ?value) => ?expected)
 :where
 | ?value               | ?expected |
 | :day                 | "DAY"     |
 | :good-to-close       | "GTC"     |
 | :immediate-or-cancel | "IOC"     |
 | :good-till-date      | "GTD"     |)

(fact "it can translate date-times to the IB format"
  (translate-to-ib-date-time (date-time 2011)) => "2011 01 01 00:00:00"
  (translate-to-ib-date-time (date-time 2001 4 1 13 30 29)) => "2001 04 01 13:30:29")

(tabular
 (fact "it can translate order actions"
   (translate-to-ib-order-action ?action) => ?expected)
 :where
 | ?action     | ?expected |
 | :buy        | "BUY"     |
 | :sell       | "SELL"    |
 | :sell-short | "SSHORT"  |)

(tabular
 (fact "it can translate to IB order types"
   (translate-to-ib-order-type ?type) => ?expected)
 :where
 | ?type  | ?expected |
 | :limit | "LMT"     |)

(tabular
 (fact "it can translate security types"
   (translate-to-ib-security-type ?type) => ?expected)
 :where
 | ?type          | ?expected |
 | :stock         | "STK"     |
 | :option        | "OPT"     |
 | :future        | "FUT"     |
 | :index         | "IND"     |
 | :future-option | "FOP"     |
 | :cash          | "CASH"    |
 | :bag           | "BAG"     |)

(tabular
 (fact "it can translate security id types"
   (translate-from-ib-security-id-type ?ib-type) => ?re-actor-type
   (translate-to-ib-security-id-type ?re-actor-type) => ?ib-type)
 :where
 | ?re-actor-type | ?ib-type |
 | :isin          | "ISIN"   |
 | :cusip         | "CUSIP"  |
 | :sedol         | "SEDOL"  |
 | :ric           | "RIC"    |)

(tabular
 (fact "it can translate tick field codes"
   (translate-from-ib-tick-field-code ?ib-code) => ?re-actor-code
   (translate-to-ib-tick-field-code ?re-actor-code) => ?ib-code)
 :where
 | ?re-actor-code | ?ib-code |
 | :bid           | 1        |
 | :ask           | 2        |
 | :last          | 4        |
 | :high          | 6        |
 | :low           | 7        |
 | :close         | 9        |)
