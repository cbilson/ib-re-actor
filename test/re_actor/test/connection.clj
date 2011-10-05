(ns re-actor.test.connection
  (:use [re-actor.connection]
        [clj-time.core :only [date-time]]
        [midje.sweet]))

#_ (process-to-message
    (.historicalData 42 "finished" 0.0 0.0 0.0 0.0 0 0 0.0 false)
    (.fooBar 1 2 3))

(defmacro process-to-messages [& forms]
  `(let [messages# (atom [])
         process-fn# (fn [message#] (swap! messages# conj message#))]
     (doto (create-client process-fn#)
       ~@forms)
     @messages#))

(fact "it handles historicalData messages from IB"
  (process-to-messages
   (.historicalData 1 "1000000000" 2.0 3.0 4.0 5.0 6 7 8.0 true))
  => [{:type :price-bar :request-id 1 :time (date-time 2001 9 9 1 46 40)
       :open 2.0 :high 3.0 :low 4.0 :close 5.0 :volume 6 :count 7 :WAP 8.0
       :has-gaps? true}])

(fact "it handles historicalData complete messages from IB"
  (process-to-messages
   (.historicalData 42 "finished" 0.0 0.0 0.0 0.0 0 0 0.0 false))
  => [{:type :complete :request-id 42}])

(fact "it handles realtime bars"
  (process-to-messages
   (.realtimeBar 51 1000000000 1.0 2.0 3.0 4.0 5 6.0 7))
  => [{:type :price-bar :request-id 51
       :time (date-time 2001 9 9 1 46 40)
       :open 1.0 :high 2.0 :low 3.0 :close 4.0 :volume 5 :count 7
       :WAP 6.0}])

(fact "it handles price ticks"
  (process-to-messages
   (.tickPrice 1 2 3.0 1))
  => [{:type :price-tick :ticker-id 1 :field :ask-price :price 3.0 :can-auto-execute? true}])

(fact "it handles size ticks"
  (process-to-messages
   (.tickSize 1 3 4))
  => [{:type :size-tick :ticker-id 1 :field :ask-size :size 4}])

(fact "it handles option computation ticks"
  (process-to-messages (.tickOptionComputation 1 10 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0))
  => [{:type :option-computation-tick :field :bid-option-computation
       :ticker-id 1
       :implied-volatility 2.0 :delta 3.0 :option-price 4.0
       :pv-dividends 5.0 :gamma 6.0 :vega 7.0 :theta 8.0 :underlying-price 9.0}])

(fact "it handles generic ticks"
  (process-to-messages (.tickGeneric 1 50 2.0))
  => [{:type :generic-tick :field :bid-yield
       :ticker-id 1 :value 2.0}])

(fact "it handles string ticks"
  (process-to-messages (.tickString 1 45 "2001-04-01 13:30:01"))
  => [{:type :string-tick :field :last-timestamp
       :ticker-id 1 :value "2001-04-01 13:30:01"}])

(fact "it handles EFP ticks"
  (process-to-messages (.tickEFP 1 38 2.0 "0.03 %" 4.0 5 "2001-04-01" 6.0 7.0))
  => [{:type :efp-tick :field :bid-efp-computation
       :ticker-id 1 :basis-points 2.0 :formatted-basis-points "0.03 %"
       :implied-future 4.0 :hold-days 5 :future-expiry "2001-04-01"
       :dividend-impact 6.0 :dividends-to-expiry 7.0}])

(fact "it handles snapshot end"
  (process-to-messages (.tickSnapshotEnd 999))
  => [{:type :tick-snapshot-end :request-id 999}])

(fact "it handles connection closed"
  (process-to-messages (.connectionClosed))
  => [{:type :connection-closed}])

(fact "it handles errors"
  (let [ex (Exception.)]
    (process-to-messages
     (.error 1 2 "some message")
     (.error "some other message")
     (.error ex))
    => [{:type :error :request-id 1 :code 2 :message "some message"}
        {:type :error :message "some other message"}
        {:type :error :exception ex}]))

(fact "it handless time messages"
  (process-to-messages (.currentTime 1000000000))
  => [{:type :current-time :value (date-time 2001 9 9 1 46 40)}])

;.;. First they ignored you, then they laughed at you, then they fought
;.;. you, now you've won. -- Not quite Gandhi
(fact "it handles order status updates"
  (process-to-messages (.orderStatus 1 "PendingSubmit" 2 3 4.0 5 6 7.0 8 "locate"))
  => [{:type :order-status :order-id 1 :status :pending-submit
       :filled 2 :remaining 3 :average-fill-price 4.0 :permanent-id 5
       :parent-id 6 :last-fill-price 7.0 :client-id 8 :why-held "locate"}])
