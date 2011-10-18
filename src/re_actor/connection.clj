(ns re-actor.connection
  (:use [re-actor.conversions])
  (:import [com.ib.client EClientSocket EWrapper]))

(defn- is-finish? [date-string]
  (.startsWith date-string "finished"))

(defn create-wrapper [process-message]
  "Creates a wrapper that flattens the Interactive Brokers EWrapper interface,
calling a single function with maps that all have a :type to indicate what type
of messages was received, and the massaged parameters from the event."
  (reify
    EWrapper

    (historicalData [this requestId date open high low close volume count wap hasGaps]
      (if (is-finish? date)
        (process-message {:type :complete :request-id requestId})
        (process-message {:type :price-bar :request-id requestId :time (translate-from-ib-date-time date)
                          :open open :high high :low low :close close :volume volume
                          :count count :WAP wap :has-gaps? hasGaps})))

    (realtimeBar [this requestId time open high low close volume wap count]
      (process-message {:type :price-bar :request-id requestId :time (translate-from-ib-date-time time)
                        :open open :high high :low low :close close :volume volume
                        :count count :WAP wap}))

    (tickPrice [this tickerId field price canAutoExecute]
      (process-message {:type :price-tick :field (translate-from-ib-tick-field-code field)
                        :ticker-id tickerId
                        :price price
                        :can-auto-execute? (= 1 canAutoExecute)}))

    (tickSize [this tickerId field size]
      (process-message {:type :size-tick :field (translate-from-ib-tick-field-code field)
                        :ticker-id tickerId
                        :size size}))
    
    (tickOptionComputation [this tickerId field impliedVol delta optPrice pvDividend gamma vega theta undPrice]
      (process-message {:type :option-computation-tick :field (translate-from-ib-tick-field-code field)
                        :ticker-id tickerId
                        :implied-volatility impliedVol
                        :option-price optPrice
                        :pv-dividends pvDividend
                        :underlying-price undPrice
                        :delta delta :gamma gamma :theta theta :vega vega }))

    (tickGeneric [this tickerId tickType value]
      (process-message {:type :generic-tick :field (translate-from-ib-tick-field-code tickType)
                        :ticker-id tickerId :value value}))

    (tickString [this tickerId tickType value]
      (let [field (translate-from-ib-tick-field-code tickType)
            val (condp = field
                  :last-timestamp (translate-from-ib-date-time value)
                  value)]
        (process-message {:type :string-tick :field field
                          :ticker-id tickerId :value val})))

    (tickEFP [this tickerId tickType basisPoints formattedBasisPoints impliedFuture holdDays futureExpiry dividendImpact dividendsToExpiry]
      (process-message {:type :efp-tick :field (translate-from-ib-tick-field-code tickType)
                        :ticker-id tickerId
                        :basis-points basisPoints :formatted-basis-points formattedBasisPoints
                        :implied-future impliedFuture :hold-days holdDays :future-expiry futureExpiry
                        :dividend-impact dividendImpact :dividends-to-expiry dividendsToExpiry}))

    (tickSnapshotEnd [this reqId]
      (process-message {:type :tick-snapshot-end :request-id reqId}))
    
    (connectionClosed [this]
      (process-message {:type :connection-closed}))
    
    (error [this requestId errorCode message]
      (process-message {:type :error :request-id requestId :code errorCode :message message}))
    
    (^void error [this ^String message]
      (process-message {:type :error :message message}))
    
    (^void error [this ^Exception ex]
      (process-message {:type :error :exception ex}))
    
    (currentTime [this time]
      (process-message {:type :current-time :value (translate-from-ib-date-time time)}))
    
    (orderStatus [this orderId status filled remaining avgFillPrice permId parentId lastFillPrice clientId whyHeld]
      (process-message {:type :order-status :order-id orderId :status (translate-from-ib-order-status status)
                        :filled filled :remaining remaining :average-fill-price avgFillPrice
                        :permanent-id permId :parent-id parentId
                        :last-fill-price lastFillPrice :client-id clientId
                        :why-held whyHeld}))
    
    (openOrder [this orderId contract order orderState]
      (process-message {:type :open-order :order-id orderId :contract contract :order order :order-state orderState}))
    
    (openOrderEnd [this]
      (process-message {:type :open-order-end}))
    
    (nextValidId [this orderId]
      (process-message {:type :next-valid-order-id :value orderId}))
    
    (updateAccountValue [this key value currency accountName]
      (let [account-value-key (translate-from-ib-account-value-key key)]
        (if (= account-value-key :day-trades-remaining)
          (process-message {:type :update-account-day-trades-remaining
                            :value (Integer/parseInt value)
                            :account accountName})
          (process-message {:type :update-account-value :key account-value-key
                            :value (Double/parseDouble value) :currency currency
                            :account accountName}))))
    
    (updatePortfolio [this contract position marketPrice marketValue averageCost unrealizedPNL realizedPNL accountName]
      (process-message {:type :update-portfolio :contract contract :position position
                        :market-price marketPrice :market-value marketValue
                        :average-cost averageCost :unrealized-gain-loss unrealizedPNL :realized-gain-loss realizedPNL
                        :account accountName}))
    
    (updateAccountTime [this timeStamp]
      (process-message {:type :update-account-time :value (translate-from-ib-date-time timeStamp)}))
    
    (contractDetails [this requestId contractDetails]
      (process-message {:type :contract-details :request-id requestId :value contractDetails}))
    
    (bondContractDetails [this requestId contractDetails]
      (process-message {:type :contract-details :request-id requestId :value contractDetails}))

    (contractDetailsEnd [this requestId]
      (process-message {:type :contract-details-end :request-id requestId}))
    
    (execDetails [this requestId contract execution]
      (process-message {:type :execution-details :request-id requestId :contract contract :value execution}))
    
    (execDetailsEnd [this requestId]
      (process-message {:type :execution-details-end :request-id requestId}))
    
    (updateMktDepth [this tickerId position operation side price size]
      (process-message {:type :update-market-depth :ticker-id tickerId :position position
                        :operation (translate-from-ib-market-depth-row-operation operation)
                        :side (translate-from-ib-market-depth-side side)
                        :price price :size size}))
    
    (updateMktDepthL2 [this tickerId position marketMaker operation side price size]
      (process-message {:type :update-market-depth-level-2 :ticker-id tickerId :position position
                        :market-maker marketMaker
                        :operation (translate-from-ib-market-depth-row-operation operation)
                        :side (translate-from-ib-market-depth-side side)
                        :price price :size size}))
    
    (updateNewsBulletin [this msgId msgType message origExchange]
      (process-message {:type (condp = msgType
                                0 :news-bulletin
                                1 :exchange-unavailable
                                2 :exchange-available)
                        :id msgId :message message :exchange origExchange}))
    
    (managedAccounts [this accountsList]
      (process-message {:type :managed-accounts
                        :accounts (->> (.split accountsList ",") (map #(.trim %)) vec)}))
    
    (receiveFA [this faDataType xml]
      (process-message {:type (condp = faDataType
                                1 :financial-advisor-groups
                                2 :financial-advisor-profile
                                3 :financial-advisor-account-aliases)
                        :value xml}))
    
    (scannerParameters [this xml]
      (process-message {:type :scan-parameters :value xml}))
    
    (scannerData [this requestId rank contractDetails distance benchmark projection legsStr]
      (process-message {:type :scan-result :request-id requestId :rank rank
                        :contract-details contractDetails :distance distance
                        :benchmark benchmark :projection projection
                        :legs legsStr}))

    (scannerDataEnd [this request-id]
      (process-message {:type :scan-end :request-id request-id}))))

(defn connect
  "This function must be called before any other. There is no feedback
for a successful connection, but a subsequent attempt to connect
will return the message 'Already connected.'

wrapper is an implementation of the EWrapper interface.

host is the hostname running IB Gateway or TWS.

port is the port IB Gateway / TWS is running on.

client-id identifies this client. Only one connection to a gateway can
be made per client-id at a time."
  ([handler-fn] (connect handler-fn "localhost"))
  ([handler-fn host] (connect handler-fn host 7496))
  ([handler-fn host port] (connect handler-fn host port 1))
  ([handler-fn host port client-id]
     (let [wrapper (create-wrapper handler-fn)
           connection (EClientSocket. wrapper)]
       (doto connection
         (.eConnect host port client-id)))))

(defn disconnect [connection]
  (.eDisconnect connection))

(defn request-market-data
  "Call this method to request market data. The market data will be returned by
:price-tick, :size-tick, :option-computation-tick, :generic-tick, :string-tick
and :efp-tick messages.

Parameter Descriptions
----------------------

connection
The connection to use to make the request. Use (connect) to get this.

tickerId
The ticker id. Must be a unique value. When the market data returns, it
will be identified by this tag. This is also used when canceling the
market data.

contract
This contains attributes used to describe the contract. Use (make-contract) or
(futures-contract) for example to create it.

tick-list (optional)
A list of tick types:
:option-volume                       Option Volume (currently for stocks)
:option-open-interest                Option Open Interest (currently for stocks)
:historical-volatility 104           Historical Volatility (currently for stocks)
:option-implied-volatility 106       Option Implied Volatility (currently for stocks)
:index-future-premium 162            Index Future Premium
:miscellaneous-stats 165             Miscellaneous Stats
:mark-price 221                      Mark Price (used in TWS P&L computations)
:auction-values 225                  Auction values (volume, price and imbalance)
:realtime-volume 233                 RTVolume
:shortable 236                       Shortable
:inventory 256                       Inventory
:fundamental-ratios 258              Fundamental Ratios
:realtime-historical-volatility 411  Realtime Historical Volatility

if no tick list is specified, a single snapshot of market data will come back
and have the market data subscription will be immediately canceled."
  ([connection id contract tick-list]
     (let [ib-tick-list (map translate-to-ib-tick-type tick-list)]
       (.reqMktData connection id contract ib-tick-list false))
     id)
  ([connection id contract]
     (.reqMktData connection id contract nil true)
     id))
