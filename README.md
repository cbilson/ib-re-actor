# ib-re-actor

A clojure friendly wrapper around the Interactive Brokers java API.

[![Build Status](https://secure.travis-ci.org/cbilson/ib-re-actor.png)](http://travis-ci.org/cbilson/ib-re-actor)

## Usage

I don't have permission to redistribute Interactive Broker's jars, so this is a little cludgey:

1. Fetch the source code for ib-re-actor from github:

```bash
> git clone http://github.com/cbilson/ib-re-actor
```

2. Download the Interactive Brokers "IB API Software" from
http://individuals.interactivebrokers.com/en/p.php?f=programInterface&ib_entity=llc. Follow
the instructions to unpack it. Use maven to install the jtsclient.jar
with groupId 'com.ib', artifactId 'jtsclient', and version '9.68'. For me, this went like:

```bash
> cd ~/Downloads

> jar xf twsapi_unixmac_968.jar

> mvn install:install-file -Dfile=IBJts/jtsclient.jar -DgroupId=com.ib -DartifactId=jtsclient -Dversion=9.68 -Dpackaging=jar
```

You can use ib-re-actor either with IB Trader Workstation or with IB
Gateway. Personally, I have the gateway running on a linux server that
I use VNC to connect to when I need to start/restart the gateway
component. I then run programs on that machine that connect to it
locally. It would be nice if there were a way to run the gateway
without X and without having to authenticate, but alas, that's not how
it works.

## Examples

### historical-data-request

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


## License

Copyright (C) 2011 Chris Bilson

Distributed under the Eclipse Public License, the same as Clojure.
