(ns re-actor.test.connection
  (:use [re-actor.connection]
        [clj-time.core :only [date-time]]
        [midje.sweet])
  (:import [com.ib.client Contract Order OrderState ContractDetails Execution]))

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

(fact "it handles order status updates"
  (process-to-messages (.orderStatus 1 "PendingSubmit" 2 3 4.0 5 6 7.0 8 "locate"))
  => [{:type :order-status :order-id 1 :status :pending-submit
       :filled 2 :remaining 3 :average-fill-price 4.0 :permanent-id 5
       :parent-id 6 :last-fill-price 7.0 :client-id 8 :why-held "locate"}])

(future-fact "it handles open updates - but I can't construct OrderStates..."
  (-> (process-to-messages (.openOrder 1 (Contract.) (Order.) (OrderState.)))  first)
  => (contains {:type :open-order :order-id 1}))

(fact "it handles order end messages"
  (process-to-messages (.openOrderEnd))
  => [{:type :open-order-end}])

(fact "it can give back the next valid id from the client"
  (process-to-messages (.nextValidId 42))
  => [{:type :next-valid-order-id :value 42}])

(fact "it can give me back updated account value"
  (process-to-messages (.updateAccountValue "CashBalance" "100.0" "ZWD" "some account this applies to"))
  => [{:type :update-account-value :key :cash-balance :value 100.0 :currency "ZWD" :account "some account this applies to"}])

(fact "it can give me back and integer value for day trades remaining for an account"
  (process-to-messages (.updateAccountValue "DayTradesRemaining" "5" nil "some account this applies to"))
  => [{:type :update-account-day-trades-remaining :value 5 :account "some account this applies to"}])

(fact "it can handle updates to the portfolio"
  (let [c1 (Contract.) c2 (Contract.) c3 (Contract.)]
    (process-to-messages (.updatePortfolio c1 1 2.0 3.0 4.0 5.0 6.0 "some account")
                         (.updatePortfolio c2 7 8.0 9.0 10.0 11.0 12.0 "some other account")
                         (.updatePortfolio c3 13 14.0 15.0 16.0 17.0 18.0 "yet another account"))
    => [{:type :update-portfolio :contract c1 :position 1 :market-price 2.0 :market-value 3.0
         :average-cost 4.0 :unrealized-gain-loss 5.0 :realized-gain-loss 6.0 :account "some account"}
        {:type :update-portfolio :contract c2 :position 7 :market-price 8.0 :market-value 9.0
         :average-cost 10.0 :unrealized-gain-loss 11.0 :realized-gain-loss 12.0 :account "some other account"}
        {:type :update-portfolio :contract c3 :position 13 :market-price 14.0 :market-value 15.0
         :average-cost 16.0 :unrealized-gain-loss 17.0 :realized-gain-loss 18.0 :account "yet another account"}]))

(fact "it can tell me the last update date of the account information"
  (process-to-messages (.updateAccountTime "1000000000"))
  => [{:type :update-account-time :value (date-time 2001 9 9 1 46 40)}])

(fact "it can relay contract details"
  (let [cd (ContractDetails.)]
    (process-to-messages (.contractDetails 1 cd))
    => [{:type :contract-details :request-id 1 :value cd}]))

(fact "it can tell me when contract details are done"
  (process-to-messages (.contractDetailsEnd 42))
  => [{:type :contract-details-end :request-id 42}])

(fact "it can give me bond contract details"
  (let [cd (ContractDetails.)]
    (process-to-messages (.bondContractDetails 1 cd))
    => [{:type :contract-details :request-id 1 :value cd}]))

(fact "it can give me execution details"
  (let [contract (Contract.)
        execution (Execution.)]
    (process-to-messages (.execDetails 1 contract execution))
    => [{:type :execution-details :request-id 1 :contract contract :value execution}]))

(fact "it can tell me when execution details are done"
  (process-to-messages (.execDetailsEnd 1))
  => [{:type :execution-details-end :request-id 1}])

(fact "it tells me when market depth changes"
  (process-to-messages (.updateMktDepth 1 2 0 1 3.0 4))
  => [{:type :update-market-depth :ticker-id 1 :position 2 :operation :insert :side :bid :price 3.0 :size 4}])

(fact "it tells me then the Level II market depth changes"
  (process-to-messages (.updateMktDepthL2 1 2 "some market maker" 1 0 3.0 4))
  => [{:type :update-market-depth-level-2 :ticker-id 1 :position 2
       :market-maker "some market maker" :operation :update :side :ask :price 3.0 :size 4}])

(future-fact "it tells me when there is a new news bulletin - BUT the compiler says that method doesn't exist"
  (process-to-messages (.updateNewsBulletin 1 0 "some message text" "some exchange")
                       (.updateNewsBulletin 2 1 "typhoon shuts down HK Exchange!!!" "HKSE")
                       (.updateNewsBulletin 3 2 "HK Exchange back in business" :exchange "HKSE"))
  => [{:type :news-bulletin :id 1 :message "some message text" :exchange "some exchange"}
      {:type :exchange-unavailable :id 2 :message "typhoon shuts down HK Exchange!!!" :exchange "HKSE"}
      {:type :exchange-available :id 3 :message "HK Exchange back in business" :exchange "HKSE"}])

(fact "it can give me a list of managed accounts"
  (process-to-messages (.managedAccounts "account1, account2, account3"))
  => [{:type :managed-accounts :accounts ["account1", "account2", "account3"]}])

(fact "it can give me Financial Advisor information"
  (process-to-messages (.receiveFA 1 "<some><group-xml /></some>")
                       (.receiveFA 2 "<some><profile-xml /></some>")
                       (.receiveFA 3 "<some><account-aliases-xml /></some>"))
  => [{:type :financial-advisor-groups :value "<some><group-xml /></some>"}
      {:type :financial-advisor-profile :value "<some><profile-xml /></some>"}
      {:type :financial-advisor-account-aliases :value "<some><account-aliases-xml /></some>"}])

(fact "it can give me valid scanner parameters"
  (process-to-messages (.scannerParameters "<some><scanner><parameters /></scanner></some>"))
  => [{:type :scan-parameters :value "<some><scanner><parameters /></scanner></some>"}])

(fact "it gives me scanner results"
  (let [cd (ContractDetails.)]
    (process-to-messages (.scannerData 1 2 cd "some distance" "some benchmark" "some projection"
                                       "some efp combo legs"))
    => [{:type :scan-result :request-id 1 :rank 2 :contract-details cd
         :distance "some distance" :benchmark "some benchmark" :projection "some projection"
         :legs "some efp combo legs"}]))

(fact "it tells me when a scan is done"
  (process-to-messages (.scannerDataEnd 1))
  => [{:type :scan-end :request-id 1}])
