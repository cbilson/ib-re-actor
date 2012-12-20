# ib-re-actor

A clojure friendly wrapper around the Interactive Brokers java API.

[![Build Status](https://secure.travis-ci.org/cbilson/ib-re-actor.png)](http://travis-ci.org/cbilson/ib-re-actor)

## Usage

In project.clj:

```clojure
(project my.project "0.0.0"
   :dependencies [[ib-re-actor "0.1.0"]]
   ...)
```

You can use ib-re-actor either with IB Trader Workstation or with IB
Gateway. Personally, I have the gateway running on a linux server that
I use VNC to connect to when I need to start/restart the gateway
component. I then run programs on that machine that connect to it
locally. It would be nice if there were a way to run the gateway
without X and without having to authenticate, but alas, that's not how
it works.

Since ib-re-actor is basically a wrapper around Interactive Brokers'
java API, [the documentation for that library][1] is often useful to have
around. It describes all the codes, order types, and the types of
errors you might get when using it.

## Examples

### Connecting

IB-React-or maintains a connection to the interactive brokers gateway
that you can share amongst many handler functions. To establish a connection:

```clojure
user> (connect)
#<Agent@3a33517f: nil>
```

In order to get called back when a message is received from the
gateway, use the subscribe function:

```clojure
user> (subscribe prn)
#<Agent@76115ae0: (#<core$prn clojure.core$prn@d1af848>)>
```

To see the callback in action, you can call something like
`request-current-time`. This sends a message to the gateway, then to
Interactive Broker servers, and eventually returns the current time to
our callback function via a :current-time message:

```clojure
user=> (request-current-time)
#<Agent@275997e4: #<EClientSocket com.ib.client.EClientSocket@5fd78173>>
user=> {:type :current-time, :value #<DateTime 2012-12-20T12:37:41.000Z>}
```

Note that the messages is rudely placed after your prompt (or if you
are using nrepl or swank in emacs, you won't see any response.) This
is because the message is dispatched on an agent thread. (If you are
using emacs, it's actually less rude, because your message will be
visible in the \*nrepl-server\* buffer, or whatever the equivalent is
in swank.)

To disconnect, simply call `disconnect`:

```clojure
user=> (disconnect)
#<Agent@275997e4: #<EClientSocket com.ib.client.EClientSocket@5fd78173>>
user=> 
```

Any commands you issue after that will get back a "Not connected"
error message:

```clojure
user=> (disconnect)
#<Agent@275997e4: #<EClientSocket com.ib.client.EClientSocket@5fd78173>>
user=> (request-current-time)
#<Agent@275997e4: #<EClientSocket com.ib.client.EClientSocket@5fd78173>>
{:type :error, :request-id -1, :code 504, :message "Not connected"}
user=> 
```

Note that there can be only one connection to the Interactive Brokers
gateway or TWS for a given client ID, so if you are writing an
application that makes multiple connections (from different
processes), you will want to come up with a way to keep the client IDs unique.

### Errors

Errors generally come back from the gateway in a message. They can be
request specific, in which case they will have a non-negative
`:request-id`, connection wide, in which case the `:request-id` will
be -1, and sometimes they may include an exception.

### Requesting Contract Information

All tradeable instruments are referred to as "contracts" in the
API documentation. Contracts are divided into a few types:

* :equity : stock, common stock, preferred stock
* :option : option contracts on stocks or other instruments
* :future : futures contracts on commodities
* :index : informational symbols, such as the value of the S&P
          500. These are generally not tradeable, but you can use the
          same API functions to get information about them as you
          would for tradeable instruments.
* :future-option : options on futures contracts
* :cash, :bag: ???

When requesting information about the contract, you need to specify a
symbol to lookup. Your options are the `:symbol` key for the general
symbol, or `:local-symbol` for an exchange specific symbol. Generally,
you also want to specify an `:exchange`, and maybe a `:currency` as
well, unless you are not sure and want more results to look for.

```clojure
user> (request-contract-details {:symbol "AAPL" :type :equity})
6
user>
;;; many, many results
...
{:type :contract-details, :request-id 6, :value {:next-option-partial false, :time-zone-id "CTT", :underlying-contract-id 0, :price-magnifier 1, :industry "Technology", :trading-hours "20121220:0830-1500;20121221:0830-1500", :long-name "APPLE INC", :convertible? false, :subcategory "Computers", :liquid-hours "20121220:0830-1500;20121221:0830-1500", :callable? false, :order-types (:ACTIVETIM :ADJUST :ALERT :ALLOC :average-cost :basket :COND :CONDORDER :DAY :DEACT :DEACTDIS :DEACTEOD :good-after-time :good-till-canceled :good-till-date :GTT :HID :limit-if-touched :limit :market-if-touched :market :market-to-limit :NONALGO :one-cancels-all :scale :SCALERST :stop :stop-limit :trail :trailing-limit-if-touched :trailing-stop-limit :trailing-market-if-touched :what-if), :valid-exchanges ["MEXI"], :min-tick 0.01, :trading-class "AAPL", :putable? false, :summary {:include-expired? false, :type :equity, :currency "MXN", :primary-exchange "MEXI", :local-symbol "AAPL", :exchange "MEXI", :symbol "AAPL", :contract-id 38708077}, :market-name "AAPL", :coupon 0.0, :category "Computers"}}
...
{:type :contract-details-end, :request-id 6}

;;; more specifically, if we were interested in trading AAPL on
;;; ISLAND:

user> (request-contract-details {:symbol "AAPL" :type :equity :exchange "ISLAND"})   

;;; only gets the one match

```

You can see all the valid exchanges for a security in the results from
a broad search and then be more specific when you actually want to
trade it.

As you can see, the response contains a `:local-symbol` which is the
same as the symbol we requested. I find this to generally be the case
with US equities. Even when the local-symbol and symbol don't match, 
you can use `:symbol` and just specify the exchange:

```clojure
user> (request-contract-details {:symbol "BP" :type :equity})

;;; lot's of matches
...
;;; say we only wanted this one:
{:type :contract-details, :request-id 11, :value { ... 
   :long-name "BANCO POPOLARE SCARL", ...
   :valid-exchanges ["SMART" "BVME" "FWB" "MIBSX" "SWB"], ...
   :summary {..., :type :equity, :currency "EUR", 
             :primary-exchange "BVME", 
             :local-symbol "B8Z", 
             :exchange "SWB", 
             :symbol "BP", ...}, ...}}
             
;;; be more specific
user> (request-contract-details {:symbol "BP" :exchange "SWB" :type :equity})

;;; only gets the one match
{... :value {... :long-name "BANCO POPLARE SCARL" ...} ...}

;;; or:
user> (request-contract-details {:local-symbol "B8Z" :type :equity})

;;; actually gets 2 matches, because Banco Poplare's local symbol is
;;; the same on both the SWB (Stuttgart) and FWB (Frankfurt)
;;; exchanges.

```

For futures, I usually find it works best to use a local symbol with
a built in expiration:

```clojure
user> (request-contract-details {:local-symbol "ESH3" :type :future})

{... :value {... :long-name "E-mini S&P 500", :contract-month "201303",
             :summary {:multiplier 50.0, :expiry #<DateTime 2013-03-15T00:00:00.000Z>, 
             :type :future, :currency "USD", :local-symbol "ESH3", 
             :exchange "GLOBEX", :symbol "ES", ..., :contract-id 98770297}, 
             :market-name "ES", ...}}

user> (request-contract-details {:local-symbol "ZN   DEC 12" :type :future})

{..., :value {... :long-name "10 Year US Treasury Note", :contract-month "201212", 
              :summary {:multiplier 1000.0, :expiry #<DateTime 2012-12-19T00:00:00.000Z>, 
              :type :future, :currency "USD", :local-symbol "ZN   DEC 12", 
              :exchange "ECBOT", :symbol "ZN", :contract-id 94977350}, 
              :market-name "ZN"}}

```

You can also use `:contract-id`, which is a unique identifier assigned
by Interactive Brokers to identify securities:

```clojure
user> (request-contract-details {:contract-id 98770297})

{... :value {... :long-name "E-mini S&P 500", :contract-month "201303",
             :summary {:multiplier 50.0, :expiry #<DateTime 2013-03-15T00:00:00.000Z>, 
             :type :future, :currency "USD", :local-symbol "ESH3", 
             :exchange "GLOBEX", :symbol "ES", ..., :contract-id 98770297}, 
             :market-name "ES", ...}}

```

## Requesting Historical Data

To get historical bars, use the `request-historical-data` function:

```clojure
user> (request-historical-data 1 {:symbol "AAPL" :type :equity :exchange "ISLAND"}
 (date-time 2012 12 18 20) 1 :day 1 :hour)
 
{:WAP 524.187, :close 521.69, :has-gaps? false, :trade-count 4538, :low 521.27, :type :price-bar, :time #<DateTime 2012-12-18T14:30:00.000Z>, :open 524.88, :high 526.35, :volume 8260, :request-id 1}
...
{:WAP 529.905, :close 530.79, :has-gaps? false, :trade-count 2563, :low 527.79, :type :price-bar, :time #<DateTime 2012-12-18T19:00:00.000Z>, :open 530.27, :high 531.64, :volume 3293, :request-id 1}
{:type :price-bar-complete, :request-id 1}

```

Note, all date-times are in UTC unless otherwise noted.

Interactive Brokers throttles historical data requests. The
restrictions at the time this was written were:
  - bar size <= 30 seconds: 6 months
  - each request must be for less than 2000 bars
  - no identical requests within 15 seconds
  - no making more than 6 requests for the same
    contract+exchange+tick-type within 2 seconds
  - no more than 60 historical data requests in a 10 minute period

In order to remain within these limits we must throttle requests for
large amounts of data. One way to do this is to break up the requests
such that you are requesting less than 2000 bars every 15
seconds. This satisfies the second requirement above and insures that
you will have a maximum of 40 requests in a 10 minute period,
satisfying the last requirement.

For example, let's say we want 1 second bars for an entire day for DOW
minis. 2000 seconds is about 33 minutes. YMZ2 trades almost all day,
but let's say we want to start 2 hours before it get's liquid until
the next time it stops trading:

```clojure
user> (def ymz2 {:type :future :local-symbol "YM   DEC 12"
                 :exchange "ECBOT"}
#'user/ymz2
user> (-> (get-contract-details ymz2) first :value
          (select-keys [:liquid-hours :trading-hours])
          pprint)
{:trading-hours "20121021:CLOSED;20121023:1700-1515",
 :liquid-hours "20121021:CLOSED;20121023:0830-1515"}
```

So let's request 1 second bars from 6:30 to 15:15. That's 9:15, or a
total of 19 requests.

This example breaks the requested period up into retrievable chunks.

```clojure
user> (def prices (atom []))

```


## License

Copyright (C) 2011 Chris Bilson

Distributed under the Eclipse Public License, the same as Clojure.

[1]: http://www.interactivebrokers.com/en/software/api/api.htm
