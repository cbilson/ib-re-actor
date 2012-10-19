(ns ib-re-actor.examples.buy-on-tick
  (:require [clj-time.core :as ctc]
            [clj-time.format :as ctf]
            [ib-re-actor.gateway :as g]))

(def k200 {:type :index :symbol "K200" :exchange "KSE"})
(def tick-kse {:type :index :symbol "TICK-KSE" :exchange "KSE"})
(def ksz2 {:type :future :local-symbol "KSZ2" :exchange "KSE"})
(def ksh3 {:type :future :local-symbol "KSH3" :exchange "KSE"})

(def orders (atom []))
(def positions (atom []))
(def ticks (atom {}))
(def tick-nasd (atom 0.0))
(def next-valid-order-id (atom -1))
(def done (promise))

(defmulti message-handler :type)

(defmethod message-handler :price-tick
  [{:keys [field contract price value size]}]
  (swap! ticks
         (fn [ticks]
           (assoc ticks contract
                  (take 10 (cons ))))
  (act))

(defmethod message-handler :string-tick [_])
(defmethod message-handler :size-tick [_])

(defmethod message-handler :error [{:keys [exception] :as msg}]
  (if (nil? exception)
    (prn msg)
    (do
      (prn exception)
      (deliver done :fail))))

(defmethod message-handler :default [msg]
  (prn msg))

(defn subscribe-to-market-data-for-all-contracts [connection]
  (doseq [contract contracts)]
    (g/request-market-data contract))

(defn -main []
  (subscribe-to-market-data-for-all-contracts connection)
  @done)

; example messages to use when not connected to a gateway
(def example-messages
  [{:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:08.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75690}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 64}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 51}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 207.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:08.000Z")}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2725.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75691}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2724.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 5}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 57}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 5}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2724.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75692}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 55}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 17}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 12}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:09.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75704}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 24}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2725.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 47}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 24}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 47}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 32}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:10.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75736}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 193.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:10.000Z")}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2725.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75738}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 29}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 30}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2725.25, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 3}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 3}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:11.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75751}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2725.0, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 18}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2725.25, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 15}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 18}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 15}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 64}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2725.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 28}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 64}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 28}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2725.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75753}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 36}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 33}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75770}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 24}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 42}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2724.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 6}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 6}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:12.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75776}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 165.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:12.000Z")}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 20}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 14}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2725.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75778}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 14}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 20}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 22}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 6}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2724.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:13.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75779}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.5, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 69}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 69}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 31}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 16}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 16}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 26}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2725.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:14.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75780}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 21}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 14}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2725.0, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 12}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2725.25, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 31}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 12}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 31}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2725.25, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75781}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 159.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:14.000Z")}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 22}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 15}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 19}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 24}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2725.5, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 5}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 5}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:16.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75788}
   {:type :price-tick, :field :high, :ticker-id 0, :price 2725.5, :can-auto-execute? false}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2725.25, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 12}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2725.5, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 58}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 12}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 58}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:16.000Z")}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75794}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 22}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 44}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75816}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 13}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 26}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 10}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 45}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 21}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 42}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2725.25, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:18.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75820}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 171.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:18.000Z")}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 43}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2725.0, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 35}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2725.25, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 35}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 35}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 35}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 24}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75844}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 39}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 9}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 42}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 24}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 28}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:19.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75872}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 129.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:20.000Z")}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 2}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:20.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75875}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2725.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75881}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 142}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2725.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 142}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 1}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2725.25, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 58}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 58}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2725.0, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 4}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 4}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 19}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:21.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75883}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 121.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:22.000Z")}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 155}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 155}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 24}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 73}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:22.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75956}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2725.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 100}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 138}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 100}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 2}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:23.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75967}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 3}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75970}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 122}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 98}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 117.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:24.000Z")}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 75971}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 126}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 109}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 136}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 31}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 123}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:25.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 76094}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 7}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:26.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77258}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 127}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 19}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 153.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:26.000Z")}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2725.0, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 7}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2725.25, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 28}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 7}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 28}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 3}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77281}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 147}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 147}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 34}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 9}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:27.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77290}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2725.0, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 7}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 7}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 25}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 4}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 36}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2725.25, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:28.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77302}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 155.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:28.000Z")}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 20}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 28}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 36}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2725.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 5}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 5}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:29.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77307}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 8}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 34}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 27}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77334}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 157}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 157}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2725.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 143}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 11}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:30.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77345}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 177.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:30.000Z")}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2725.0, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 9}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2725.25, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 37}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 9}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 37}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 15}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77360}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 131}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2725.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 131}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 132}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 17}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2724.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:32.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77361}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 169.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:32.000Z")}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 117}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 37}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 3}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77364}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2725.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:33.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77365}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 121}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 33}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 4}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:34.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77369}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 179.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:34.000Z")}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 129}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 44}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77371}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2724.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 4}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 4}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:35.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77375}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.5, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 35}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2724.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 69}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 35}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 69}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2724.5, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77376}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.25, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 88}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 88}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 35}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 87}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 45}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.5, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 5}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 5}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 46}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.25, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 10}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2724.5, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 36}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 10}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 36}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2724.25, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 5}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 5}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:36.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77381}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 132.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:36.000Z")}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 44}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77425}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 8}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 42}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.0, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 38}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2724.25, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 10}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 38}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 10}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 27}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:38.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77452}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 62.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:38.000Z")}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2724.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77453}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2724.25, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 2}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:39.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77455}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.25, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 17}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2724.5, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 33}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 17}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 33}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 6}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 23}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 25}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 21}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77476}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 54.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:40.000Z")}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.0, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 58}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 58}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 35}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 11}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:40.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77487}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.25, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 38}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.5, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 2}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2724.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 59}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 59}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2724.5, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 4}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 4}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:42.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77491}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 62.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:42.000Z")}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2724.0, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 46}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2724.25, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 9}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 46}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 9}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2724.25, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77492}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 36}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 8}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 20}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 21}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 12}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:43.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77504}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 18}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 37}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 60.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:44.000Z")}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2724.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:44.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77505}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 22}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 25}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 10}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77515}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 25}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 27}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 50.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:46.000Z")}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 19}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 36}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 2}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:47.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77517}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2723.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 106}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 106}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 38}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2724.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 5}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 95}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 5}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 45}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 71}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2723.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77518}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 18.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:48.000Z")}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 96}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.25, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 96}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 1}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2723.25, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:48.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77519}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 71}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 63}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 71}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 63}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 10}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 10}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77600}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2723.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 5}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 5}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:49.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77605}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.25, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 39}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 64}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 39}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 8}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:50.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77674}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 53}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 33}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price -19.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:50.000Z")}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 6}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 6}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 27}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2723.25, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:51.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77680}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.5, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 47}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 34}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 47}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.25, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 59}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 18}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77698}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 58}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 33}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 19.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:52.000Z")}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 41}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 44}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2723.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 3}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 3}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:53.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77701}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 38}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 23}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 38}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 23}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 24}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 33}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:54.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77747}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.5, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 11}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 39}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 11}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 39}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 17.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:54.000Z")}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 27}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 34}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 17}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 45}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.5, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:55.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77748}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 5}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 42}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 11}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 40}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.25, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 59}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.5, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 5}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 59}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 5}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.25, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77749}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.5, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 4}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 4}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:56.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 77753}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.5, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 12}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 38}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 12}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 38}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 18}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 27}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78133}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price -15.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:56.000Z")}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 63}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 21}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 63}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 16}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78149}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 34}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 5}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 34}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.5, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:57.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78150}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.25, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 54}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 54}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 52}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 14}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78164}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.25, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78165}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.5, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 3}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 3}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 42}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 2}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 40}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 40}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:58.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78166}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price -7.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:55:58.000Z")}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 17}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 11}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2723.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78167}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 23}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:55:59.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78181}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 31}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.25, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 13}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 31}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 13}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2723.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78182}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 40}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 6}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 40}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 6}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 40}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:00.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78222}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 26}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 12}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price -3.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:00.000Z")}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 10}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 24}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.5, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 43}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 3}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 43}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 3}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78223}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 1}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 42}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 42}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:01.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78228}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.5, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 25}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 25}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 30}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 3}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 33}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 3}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 25}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.5, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:02.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78230}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price -17.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:02.000Z")}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 3}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78233}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 7}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 23}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 15}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 24}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:03.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78234}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 1}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 45}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 45}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.5, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 37}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 37}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 48}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 50}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 44}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2723.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:04.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78235}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 13}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 10}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price -47.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:04.000Z")}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 12}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 17}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 20}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 13}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 23}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78258}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 16}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 26}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 3}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 17}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 13}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 41}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:05.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78299}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.5, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 40}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 40}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 1}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:06.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78300}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price -31.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:06.000Z")}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 23}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 15}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.25, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 21}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.5, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 34}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 21}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 34}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.25, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 6}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 6}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:07.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78306}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 14}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78320}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 32}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 30}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price -39.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:08.000Z")}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 21}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 39}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 17}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 15}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:09.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78335}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.5, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 6}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 6}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78341}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 40}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 36}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 40}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.5, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 17}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 30}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 17}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 11}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78352}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:10.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78354}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 21}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 24}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price -53.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:10.000Z")}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 38}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 3}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.25, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:11.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78361}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 25}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 18}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 29}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 9}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.5, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:12.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78362}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 60}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 39}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 60}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price -45.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:12.000Z")}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.5, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 19}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 19}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 8}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78363}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 62}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:13.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78425}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 18}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 15}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 22}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 8}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 31.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:14.000Z")}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 4}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:14.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78429}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 51}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 28}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 51}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 24}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 20}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 19}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.5, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:15.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78430}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 16}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 31}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 21.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:16.000Z")}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 11}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78441}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 23}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 23}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:17.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78442}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 61}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 27}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 61}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2723.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:18.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78443}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 14.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:18.000Z")}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 25}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 25}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 26}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 5}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:19.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78448}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 35}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.25, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 28}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 35}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 28}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 34}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 11}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 34}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 11}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 10}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78458}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 50.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:20.000Z")}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 9}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 32}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:21.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78459}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 12}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 27}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 18}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 21}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 23}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 12}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 42.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:22.000Z")}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 4}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 4}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:22.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78463}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.5, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 44}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 12}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 44}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 30}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 61}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 30}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 27}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 24}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 27}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.5, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 3}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 3}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:23.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78486}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 55}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 34}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 55}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 8}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 8}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:24.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78494}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 11.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:24.000Z")}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 12}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78506}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 39}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 52}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 7}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 7}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.5, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:26.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78507}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78735}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 59}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 59}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 25.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:26.000Z")}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 42}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 2}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 2}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78737}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 61}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 36}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 61}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.5, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 47}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 47}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:27.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78789}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 32}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 12}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 32}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78790}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 6}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price 33.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:28.000Z")}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 13}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 29}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 2}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:28.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78792}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 32}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 36}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 32}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 26}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:29.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78818}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 8}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 8}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 23}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price -15.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:30.000Z")}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2723.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 3}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 3}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:30.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78821}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 3}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 24}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2722.75, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 6}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 6}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:31.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78831}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.5, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 20}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 11}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 20}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 11}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 22}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 4}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 13}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78844}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price -3.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:32.000Z")}
   {:type :price-tick, :field :ask-price, :ticker-id 0, :price 2723.0, :can-auto-execute? true}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 57}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 23}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 57}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 4}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:32.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78848}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 30}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 47}
   {:type :price-tick, :field :last-price, :ticker-id 0, :price 2723.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :last-size, :ticker-id 0, :size 1}
   {:type :string-tick, :field :last-timestamp, :ticker-id 0, :value (clj-time.format/parse "2012-04-12T13:56:33.000Z")}
   {:type :size-tick, :field :volume, :ticker-id 0, :size 78849}
   {:type :price-tick, :field :bid-price, :ticker-id 0, :price 2722.75, :can-auto-execute? true}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :bid-size, :ticker-id 0, :size 1}
   {:type :size-tick, :field :ask-size, :ticker-id 0, :size 46}
   {:type :price-tick, :field :last-price, :ticker-id 1, :price -9.0, :can-auto-execute? false}
   {:type :size-tick, :field :last-size, :ticker-id 1, :size 0}
   {:type :string-tick, :field :last-timestamp, :ticker-id 1, :value (clj-time.format/parse "2012-04-12T13:56:34.000Z")}])

(defn playback-examples []
  (doseq [msg example-messages]
    (message-handler msg)))
