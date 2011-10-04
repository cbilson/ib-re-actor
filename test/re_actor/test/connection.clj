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
  => [{:ticker-id 1 :field :ask :price 3.0 :can-auto-execute? true}])

