(ns ib-re-actor.test.gateway
  (:use [ib-re-actor.gateway]
        [ib-re-actor.translation]
        [ib-re-actor.mapping]
        [clj-time.core :only [date-time]]
        [midje.sweet]
        [midje.util :only [testable-privates]])
  (:import [com.ib.client Contract Order OrderState ContractDetails Execution]))

(testable-privates ib-re-actor.gateway dispatch-message create-wrapper)

(def some-contract {:symbol "SOME TICKER"})
(def some-contract-id (register-contract some-contract))

(defn make-order-state []
  (let [ctor (first (.getDeclaredConstructors OrderState))]
    (.setAccessible ctor true)
    (.newInstance ctor nil)))

(defmacro wrapper->message
  "Given 1 or more wrapper calls, creates a wrapper and applies the method calls to the
wrapper, collecting and returning any messages the wrapper dispatched."
  [& calls]
  (let [wrapper (gensym "wrapper")]
    `(let [messages# (atom nil)
           ~wrapper (create-wrapper)]
       (with-redefs [ib-re-actor.gateway/dispatch-message
                     (fn [m#] (swap! messages# conj m#))]
         ~@(map #(concat [`. wrapper] %) calls))
       (first @messages#))))

(fact "when IB sends the current time, it dispatches a current time message"
      (wrapper->message (currentTime 1000000000))
      => {:type :current-time :value (date-time 2001 9 9 1 46 40)})

(fact "historicalData messages from IB"
      (wrapper->message (historicalData 1 "1000000000" 2.0 3.0 4.0 5.0 6 7 8.0 true))
      => {:type :price-bar :request-id 1 :time (date-time 2001 9 9 1 46 40)
          :open 2.0 :high 3.0 :low 4.0 :close 5.0 :volume 6 :trade-count 7 :WAP 8.0
          :has-gaps? true})

(fact "historicalData complete messages from IB"
      (wrapper->message
       (historicalData 1 "finished" 0.0 0.0 0.0 0.0 0 0 0.0 false))
      => {:type :price-bar-complete :request-id 1})

(fact "realtime bars"
      (wrapper->message (realtimeBar 1 1000000000
                                     1.0 2.0 3.0 4.0 5 6.0 7))
      => {:type :price-bar :request-id 1
          :time (date-time 2001 9 9 1 46 40)
          :open 1.0 :high 2.0 :low 3.0 :close 4.0 :volume 5 :count 7
          :WAP 6.0})

(fact "price ticks"
      (wrapper->message (tickPrice some-contract-id 2 3.0 1))
      => {:type :tick :field :ask-price :value 3.0
          :can-auto-execute? true :contract some-contract})

(fact "size ticks"
      (wrapper->message (tickSize some-contract-id 3 4))
      => {:type :tick :field :ask-size :value 4
          :contract some-contract})

(fact "option computation ticks"
      (wrapper->message
       (tickOptionComputation some-contract-id 10 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0))
      => {:type :tick :field :bid-option-computation :contract some-contract
          :implied-volatility 2.0 :delta 3.0 :option-price 4.0
          :pv-dividends 5.0 :gamma 6.0 :vega 7.0 :theta 8.0 :underlying-price 9.0})

(fact "generic ticks"
      (wrapper->message (tickGeneric some-contract-id 50 2.0))
      => {:type :tick :field :bid-yield
          :contract some-contract :value 2.0})

(fact "string ticks"
      (fact "last timestamp ticks"
            (wrapper->message
             (tickString some-contract-id 45 "1000000000"))
            => {:type :tick :field :last-timestamp
                :contract some-contract
                :value (date-time 2001 9 9 1 46 40)}))

(fact "EFP ticks"
      (wrapper->message (tickEFP some-contract-id 38 2.0 "0.03 %" 4.0 5
                                 "2001-04-01" 6.0 7.0))
      => {:type :tick :field :bid-efp-computation
          :contract some-contract :basis-points 2.0
          :formatted-basis-points "0.03 %"
          :implied-future 4.0 :hold-days 5 :future-expiry "2001-04-01"
          :dividend-impact 6.0 :dividends-to-expiry 7.0})

(fact "snapshot end"
      (wrapper->message (tickSnapshotEnd 1))
      => {:type :tick-snapshot-end :request-id 1})

(fact "connection closed"
      (wrapper->message (connectionClosed))
      => {:type :connection-closed})

(fact "errors"
      (fact "specific to a particular request"
            (wrapper->message (error 1 99999 "some message"))
            => {:type :error :request-id 1 :code 99999 :message "some message"})
      (fact "just a message"
            (wrapper->message (error "some message"))
            => {:type :error :message "some message"})
      (fact "exceptions"
            (let [ex (Exception. "some problem")]
              (wrapper->message (error ex))
              => {:type :error :exception "java.lang.Exception: some problem"})))

(fact "time messages"
      (wrapper->message (currentTime 1000000000))
      => {:type :current-time :value (date-time 2001 9 9 1 46 40)})

(fact "order status updates"
      (wrapper->message (orderStatus 1 "PendingSubmit" 2 3 4.0 5 6 7.0 8 "locate"))
      => {:type :order-status :order-id 1
          :status :pending-submit
          :filled 2 :remaining 3 :average-fill-price 4.0 :permanent-id 5
          :parent-id 6 :last-fill-price 7.0 :client-id 8
          :why-held "locate"})

(fact "open order updates"
      (let [order (Order.)
            mapped-order (->map order)
            order-state (make-order-state)
            mapped-order-state (->map order-state)
            contract (Contract.)
            mapped-contract (->map contract)]
        (wrapper->message (openOrder 1 contract order order-state))
        => {:type :open-order :order-id 1 :contract mapped-contract
            :order mapped-order :order-state mapped-order-state}))

(fact "order end messages"
      (wrapper->message (openOrderEnd))
      => {:type :open-order-end})

(fact "next valid id"
      (wrapper->message (nextValidId 42))
      => {:type :next-valid-order-id :value 42})

(fact "updating account value"
      (fact "integer account value"
            (wrapper->message
             (updateAccountValue "DayTradesRemaining" "5" nil "some account"))
            => {:type :update-account-value :key :day-trades-remaining
                :value 5 :currency nil :account "some account"})
      (fact "numeric account value"
            (wrapper->message
             (updateAccountValue "CashBalance" "123.456" "ZWD" "some account"))
            => {:type :update-account-value :key :cash-balance
                :value 123.456 :currency "ZWD" :account "some account"})
      (fact "boolean account value"
            (fact "true value"
                  (wrapper->message
                   (updateAccountValue "AccountReady" "true" nil "some account"))
                  => {:type :update-account-value :key :account-ready
                      :value true :currency nil :account "some account"})
            (fact "false value"
                  (wrapper->message
                   (updateAccountValue "AccountReady" "false" nil "some account"))
                  => {:type :update-account-value :key :account-ready
                      :value false :currency nil :account "some account"}))
      (fact "other type of account value"
            (wrapper->message
             (updateAccountValue "AccountCode" "some code" nil "some account"))
            => {:type :update-account-value :key :account-code
                :value "some code" :currency nil :account "some account"}))

(fact "updates to portfolio"
      (let [contract (Contract.)
            mapped-contract (->map contract)]
        (wrapper->message (updatePortfolio contract 1 2.0 3.0 4.0 5.0 6.0 "some account"))
        => {:type :update-portfolio :contract mapped-contract :position 1
            :market-price 2.0 :market-value 3.0 :average-cost 4.0
            :unrealized-gain-loss 5.0 :realized-gain-loss 6.0
            :account "some account"}))

(fact "last update date of the account information"
      (wrapper->message (updateAccountTime "13:45"))
      => {:type :update-account-time :value (date-time 1970 1 1 13 45)})

(fact "contract details"
      (let [cd (ContractDetails.)
            mapped-cd (->map cd)]
        (wrapper->message (contractDetails 1 cd))
        => {:type :contract-details :request-id 1 :value mapped-cd}))

(fact "when contract details are done"
      (wrapper->message (contractDetailsEnd 42))
      => {:type :contract-details-end :request-id 42})

(fact "it can give me bond contract details"
      (let [cd (ContractDetails.)
            mapped-cd (->map cd)]
        (wrapper->message (bondContractDetails 1 cd))
        => {:type :contract-details :request-id 1 :value mapped-cd}))

(fact "execution details"
      (let [contract (Contract.)
            mapped-contract (->map contract)
            execution (Execution.)
            mapped-execution (->map execution)]
        (wrapper->message (execDetails 1 contract execution))
        => {:type :execution-details :request-id 1
            :contract mapped-contract :value mapped-execution}))

(fact "when execution details are done"
      (wrapper->message (execDetailsEnd 1))
      => {:type :execution-details-end :request-id 1})

(fact "when market depth changes"
      (wrapper->message (updateMktDepth some-contract-id 2 0 1 3.0 4))
      => {:type :update-market-depth :contract some-contract :position 2
          :operation :insert :side :bid :price 3.0 :size 4})

(fact "when the Level II market depth changes"
      (wrapper->message (updateMktDepthL2 some-contract-id 2 "some market maker"
                                          1 0 3.0 4))
      => {:type :update-market-depth-level-2 :contract some-contract :position 2
          :market-maker "some market maker" :operation :update :side :ask
          :price 3.0 :size 4})

(fact "when there is a new news bulletin"
      (fact "in general"
            (wrapper->message (updateNewsBulletin 1 0 "some message text" "some exchange"))
            => {:type :news-bulletin :id 1 :message "some message text"
                :exchange "some exchange"})
      (fact "saying an exchange in unavailable"
            (wrapper->message (updateNewsBulletin 2 1 "typhoon shuts down HK Exchange!!!"
                                                  "HKSE"))
            => {:type :exchange-unavailable :id 2
                :message "typhoon shuts down HK Exchange!!!"
                :exchange "HKSE"})
      (fact "saying an exchange is available again"
            (wrapper->message (updateNewsBulletin 3 2 "HK Exchange back in business"
                                                  "HKSE"))
            => {:type :exchange-available :id 3
                :message "HK Exchange back in business"
                :exchange "HKSE"}))

(fact "getting a list of managed accounts"
      (wrapper->message (managedAccounts "account1, account2, account3"))
      => {:type :managed-accounts :accounts ["account1", "account2", "account3"]})

(fact "getting Financial Advisor information"
      (fact "groups"
            (wrapper->message (receiveFA 1 "<some><group-xml /></some>"))
            => {:type :financial-advisor-groups :value "<some><group-xml /></some>"})
      (fact "profile"
            (wrapper->message (receiveFA 2 "<some><profile-xml /></some>"))
            => {:type :financial-advisor-profile :value "<some><profile-xml /></some>"})
      (fact "account aliases"
            (wrapper->message (receiveFA 3 "<some><account-aliases-xml /></some>"))
            => {:type :financial-advisor-account-aliases
                :value "<some><account-aliases-xml /></some>"}))

(fact "getting valid scanner parameters"
      (wrapper->message
       (scannerParameters "<some><scanner><parameters /></scanner></some>"))
      => {:type :scan-parameters :value "<some><scanner><parameters /></scanner></some>"})

(fact "getting scanner results"
      (let [cd (ContractDetails.)
            mapped-cd (->map cd)]
        (wrapper->message (scannerData 1 2 cd "some distance" "some benchmark"
                                       "some projection" "some efp combo legs"))
        => {:type :scan-result :request-id 1 :rank 2 :contract-details mapped-cd
            :distance "some distance" :benchmark "some benchmark"
            :projection "some projection" :legs "some efp combo legs"}))

(fact "when a scan is done"
      (wrapper->message (scannerDataEnd 1))
      => {:type :scan-end :request-id 1})

(fact "finding the end of a stream of messages"
      (fact "when not at the end"
            (is-end-for? 1 {:not :end}) => false)
      (fact "when it's a serious error for everyone"
            (is-end-for? 1 {:type :error :code 1}) => true)
      (fact "when it's a non-serious error for everyone"
            (is-end-for? 1 {:type :error :code 2100}) => false)
      (fact "when it's a serious error for another request"
            (is-end-for? 1 {:type :error :code 1 :request-id 2})  => false)
      (fact "when it's a serious error for this request"
            (is-end-for? 1 {:type :error :code 1 :request-id 1}) => true)
      (fact "when it's a non-serious error for this request"
            (is-end-for? 1 {:type :error :code 2100 :request-id 1}) => false))
