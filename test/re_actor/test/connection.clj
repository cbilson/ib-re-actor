(ns re-actor.test.connection
  (:use [re-actor.connection]
        [clj-time.core :only [date-time]]
        [midje.sweet]))

(def *messages* (atom []))

(defn process-message [message]
  (swap! *messages* conj message))

(binding [*messages* (atom [])]
  (fact "it handles historicalData messages from IB"
    (let [client (create-client process-message)
          request-id 42
          timestamp "1000000000"
          open 1.0
          high 1.5
          low 0.5
          close 0.8
          volume 50
          count 5
          wap 0.9
          has-gaps? true]
      (.historicalData client request-id timestamp open high low close volume count wap has-gaps?)
      @*messages* => [{:type :price-bar
                       :request-id request-id
                       :time (date-time 2001 9 9 1 46 40)
                       :open open
                       :high high
                       :low low
                       :close close
                       :volume volume
                       :count count
                       :WAP wap
                       :has-gaps? has-gaps?}])))

(binding [*messages* (atom [])]
  (fact "it handles historicalData complete messages from IB"
    (let [client (create-client process-message)]
      (.historicalData client 42 "finished" 0.0 0.0 0.0 0.0 0 0 0.0 false)
      @*messages* => [{:type :complete :request-id 42}])))

;.;. Excellence is not an act but a habit. -- Aristotle
(binding [*messages* (atom [])]
  (fact "it handles realtime bars"
    (let [client (create-client process-message)]
      (.realtimeBar client 51 1000000000 1.0 2.0 3.0 4.0 5 6.0 7)
      @*messages* => [{:type :price-bar :request-id 51
                       :time (date-time 2001 9 9 1 46 40)
                       :open 1.0 :high 2.0 :low 3.0 :close 4.0 :volume 5 :count 7
                       :WAP 6.0}])))
