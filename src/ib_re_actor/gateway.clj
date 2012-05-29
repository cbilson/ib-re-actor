(ns ib-re-actor.gateway
  "Functions for connecting to Interactive Brokers TWS Gateway and sending requests to it."
  (:use [ib-re-actor.translation :only [translate integer-account-value? numeric-account-value? boolean-account-value?]]
        [ib-re-actor.mapping])
  (:require [clojure.xml :as xml]))

(def ^:dynamic *client-id* 100)
(def ^:dynamic *next-order-id* (atom 0))
(def ^:dynamic *next-request-id* (atom 0))
(def ^:dynamic *default-server-log-level* :error)
(def ^:dynamic *prn-raw-messages* false)

(defn get-order-id []
  (swap! *next-order-id* inc))

(defn get-request-id []
  (swap! *next-request-id* inc))

(defprotocol PricingDataProvider
  (request-market-data
    [this id contract tick-list snapshot?]
    [this id contract]
    "Call this function to request market data. The market data will be returned by
   :price-tick, :size-tick, :option-computation-tick, :generic-tick, :string-tick
   and :efp-tick messages.

   For snapshots, a :tick-snapshot-end message will indicate the snapshot is done.

   ## Parameters
   - this
     The connection to use to make the request. Use (connect) to get this.

   - tickerId
     The ticker id. Must be a unique value. When the market data returns, it
     will be identified by this tag. This is also used when canceling the
     market data.

   - contract
     This contains attributes used to describe the contract. Use (make-contract) or
     (futures-contract) for example to create it.

   - tick-list (optional)
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
     and have the market data subscription will be immediately canceled.")

  (request-historical-data
    [this id contract end duration duration-unit bar-size bar-size-unit what-to-show use-regular-trading-hours?]
    [this id contract end duration duration-unit bar-size bar-size-unit what-to-show]
    [this id contract end duration duration-unit bar-size bar-size-unit]
    "Start receiving historical price bars stretching back <duration> <duration-unit>s back,
   up till <end> for the specified contract. The messages will have :request-id of <id>.

   duration-unit should be one of :second(s), :day(s), :week(s), or :year(s).

   bar-size-unit should be one of :second(s), :minute(s), :hour(s), or :day(s).

   what-to-show should be one of :trades, :midpoint, :bid, :ask, :bid-ask, :historical-volatility,
   :option-implied-volatility, :option-volume, or :option-open-interest.")

  (request-real-time-bars
    [this id contract what-to-show use-regular-trading-hours?]
    "Start receiving real time bar results."))

(defprotocol MarketNewsProvider
  (request-news-bulletins
    [this]
    [this all-messages?]
    "Call this function to start receiving news bulletins. Each bulletin will
   be sent in a :news-bulletin, :exchange-unavailable, or :exchange-available
   message.")

  (cancel-news-bulletins
    [this]
    "Call this function to stop receiving news bulletins."))

(defprotocol SecurityDataProvider
  (request-fundamental-data
    [this request-id contract report-type]
    [this contract report-type]
    "Call this function to receive Reuters global fundamental data. There must be a
   subscription to Reuters Fundamental set up in Account Management before you
   can receive this data.")

  (cancel-fundamental-data
    "Call this function to stop receiving Reuters global fundamental data."
    [this request-id])

  (request-contract-details
    [this request-id contract]
    [this contract]))

(defprotocol OrderManager
  (place-order [this contract order] [this order-id contract order])
  (cancel-order [this order-id])
  (request-open-orders [this])
  (request-executions [this] [this client-id]))

(defprotocol AccountDataProvider
  (request-account-updates [this subscribe? account-code]))

(defprotocol Connection
  (disconnect
    [this]
    "Call this function to terminate the connections with TWS.
   Calling this function does not cancel orders that have already been sent.")
  (set-server-log-level [this log-level]
    "Call this function to set the log level used on the server.")
  (request-current-time [this])
  (is-connected? [this])
  (connection-time [this])
  (server-version [this]))

(defn- is-finish? [date-string]
  (.startsWith date-string "finished"))

(defn is-end?
  "Predicate to determine if a message indicates a tick snapshot is done"
  [msg]
  (contains? [:tick-snapshot-end :error]))

(defmulti warning? class)

(defmethod warning? java.lang.Integer [code]
  (>= code 2100))

(defmethod warning? java.lang.Long [code]
  (>= code 2100))

(defmethod warning? clojure.lang.IPersistentMap [{code :code exception :exception}]
  (cond
   (not (nil? exception)) false
   (nil? code) false
   :else (warning? code)))

(defmethod warning? :default [_]
  false)

(def error? (comp not warning?))

;; I found having to implement the entire `EWrapper` interface to be
;; kind of a pain, so I decided to try flattening it into something
;; that would be easy to use either via multi-methods or that you
;; could build some other protocol on top of.

;; Another consideration is that this would be a great point to build
;; a layer that talks directly to TWS, not using the jtsclient stuff,
;; like what was done for the .NET TWS API. I think you could take
;; advantage of clojure's transactional memory better if you did that.

(defn- create-wrapper
  "Creates a wrapper that flattens the Interactive Brokers EWrapper interface,
   calling a single function with maps that all have a :type to indicate what type
   of messages was received, and the massaged parameters from the event."
  [process-message]
  (reify
    com.ib.client.EWrapper
    (historicalData [this requestId date open high low close volume count wap hasGaps]
      (if (is-finish? date)
        (process-message {:type :complete :request-id requestId})
        (process-message {:type :price-bar :request-id requestId
                          :time (translate :from-ib :date-time date)
                          :open open :high high :low low :close close :volume volume
                          :trade-count count :WAP wap :has-gaps? hasGaps})))

    (realtimeBar [this requestId time open high low close volume wap count]
      (process-message {:type :price-bar :request-id requestId
                        :time (translate :from-ib :date-time time)
                        :open open :high high :low low :close close :volume volume
                        :count count :WAP wap}))

    (tickPrice [this tickerId field price canAutoExecute]
      (process-message {:type :tick :field (translate :from-ib :tick-field-code field)
                        :ticker-id tickerId
                        :value price
                        :can-auto-execute? (= 1 canAutoExecute)}))

    (tickSize [this tickerId field size]
      (process-message {:type :tick :field (translate :from-ib :tick-field-code field)
                        :ticker-id tickerId :value size}))

    (tickOptionComputation [this tickerId field impliedVol delta optPrice pvDividend gamma vega theta undPrice]
      (process-message {:type :tick
                        :field (translate :from-ib :tick-field-code field)
                        :ticker-id tickerId
                        :implied-volatility impliedVol
                        :option-price optPrice
                        :pv-dividends pvDividend
                        :underlying-price undPrice
                        :delta delta :gamma gamma :theta theta :vega vega}))

    (tickGeneric [this tickerId tickType value]
      (process-message {:type :tick
                        :field (translate :from-ib :tick-field-code tickType)
                        :ticker-id tickerId :value value}))

    (tickString [this tickerId tickType value]
      (let [field (translate :from-ib :tick-field-code tickType)]
        (cond
         (= field :last-timestamp)
         (process-message {:type :timestamp-tick :field field
                           :ticker-id tickerId
                           :value (translate :from-ib :date-time value)})

         :else
         (process-message {:type :string-tick :field field
                           :ticker-id tickerId
                           :value val}))))

    (tickEFP [this tickerId tickType basisPoints formattedBasisPoints impliedFuture holdDays futureExpiry dividendImpact dividendsToExpiry]
      (process-message {:type :tick
                        :field (translate :from-ib :tick-field-code tickType)
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
      (process-message {:type :current-time :value (translate :from-ib :date-time time)}))

    (orderStatus [this orderId status filled remaining avgFillPrice permId parentId lastFillPrice clientId whyHeld]
      (process-message {:type :order-status :order-id orderId
                        :status (translate :from-ib :order-status status)
                        :filled filled :remaining remaining
                        :average-fill-price avgFillPrice
                        :permanent-id permId :parent-id parentId
                        :last-fill-price lastFillPrice :client-id clientId
                        :why-held whyHeld}))

    (openOrder [this orderId contract order orderState]
      (process-message {:type :open-order :order-id orderId :contract (->map contract)
                        :order (->map order) :order-state (->map orderState)}))

    (openOrderEnd [this]
      (process-message {:type :open-order-end}))

    (nextValidId [this orderId]
      (dosync
       (reset! *next-order-id* orderId))
      (process-message {:type :next-valid-order-id :value orderId}))

    (updateAccountValue [this key value currency accountName]
      (let [avk (translate :from-ib :account-value-key key)
            val (cond
                 (integer-account-value? avk) (Integer/parseInt value)
                 (numeric-account-value? avk) (Double/parseDouble value)
                 (boolean-account-value? avk) (Boolean/parseBoolean value)
                 :else value)]
        (process-message {:type :update-account-value :key avk :value val :currency currency :account accountName})))

    (accountDownloadEnd [this account-code]
      (process-message {:type :account-download-end :account-code account-code}))

    (updatePortfolio [this contract position marketPrice marketValue averageCost unrealizedPNL realizedPNL accountName]
      (process-message {:type :update-portfolio :contract (->map contract) :position position
                        :market-price marketPrice :market-value marketValue
                        :average-cost averageCost :unrealized-gain-loss unrealizedPNL
                        :realized-gain-loss realizedPNL
                        :account accountName}))

    (updateAccountTime [this timeStamp]
      (process-message {:type :update-account-time
                        :value (translate :from-ib :time-of-day timeStamp)}))

    (contractDetails [this requestId contractDetails]
      (process-message {:type :contract-details :request-id requestId
                        :value (->map contractDetails)}))

    (bondContractDetails [this requestId contractDetails]
      (process-message {:type :contract-details :request-id requestId :value contractDetails}))

    (contractDetailsEnd [this requestId]
      (process-message {:type :contract-details-end :request-id requestId}))

    (execDetails [this requestId contract execution]
      (process-message {:type :execution-details :request-id requestId :contract (-> map contract) :value execution}))

    (execDetailsEnd [this requestId]
      (process-message {:type :execution-details-end :request-id requestId}))

    (updateMktDepth [this tickerId position operation side price size]
      (process-message {:type :update-market-depth :ticker-id tickerId :position position
                        :operation (translate :from-ib :market-depth-row-operation operation)
                        :side (translate :from-ib :market-depth-side side)
                        :price price :size size}))

    (updateMktDepthL2 [this tickerId position marketMaker operation side price size]
      (process-message {:type :update-market-depth-level-2 :ticker-id tickerId :position position
                        :market-maker marketMaker
                        :operation (translate :from-ib :market-depth-row-operation operation)
                        :side (translate :from-ib :market-depth-side side)
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
                        :contract-details (->map contractDetails) :distance distance
                        :benchmark benchmark :projection projection
                        :legs legsStr}))

    (scannerDataEnd [this requestId]
      (process-message {:type :scan-end :request-id requestId}))

    (fundamentalData [this requestId xml]
      (process-message {:type :fundamental-data :request-id requestId
                        :report (xml/parse (java.io.ByteArrayInputStream. (.getBytes xml)))}))))

(extend-type com.ib.client.EClientSocket
  Connection
  (disconnect [this]
    (.eDisconnect this)
    nil)
  (set-server-log-level [this level]
    (.setServerLogLevel this (translate :to-ib :log-level level)))
  (request-current-time [this]
    (.requestCurrentTime this))
  (is-connected? [this]
    (.isConnected this))
  (connection-time [this]
    (->> (.TwsConnectionTime this)
         (translate :from-ib :connection-time)))
  (server-version [this]
    (.serverVersion this))

  PricingDataProvider
  (request-market-data
    ([this id contract tick-list snapshot?]
       (let [ib-tick-list (translate :to-ib :tick-list tick-list)]
         (.reqMktData this id (map-> com.ib.client.Contract contract) ib-tick-list snapshot?))
       id)
    ([this id contract]
       (.reqMktData this id (map-> com.ib.client.Contract) contract "" false)
       id))

  (request-historical-data
    ([this id contract end duration duration-unit bar-size bar-size-unit what-to-show use-regular-trading-hours?]
       (let [ib-end (translate :to-ib :date-time end)
             ib-duration (translate :to-ib :duration [duration duration-unit])
             ib-bar-size (translate :to-ib :bar-size [bar-size bar-size-unit])
             ib-what-to-show (translate :to-ib :what-to-show what-to-show)]
         (.reqHistoricalData this id (map-> com.ib.client.Contract contract) ib-end ib-duration ib-bar-size ib-what-to-show
                             (if use-regular-trading-hours? 1 0)
                             2)))
    ([this id contract end duration duration-unit bar-size bar-size-unit what-to-show]
       (request-historical-data this id contract end duration duration-unit bar-size bar-size-unit what-to-show true))
    ([this id contract end duration duration-unit bar-size bar-size-unit]
       (request-historical-data this id contract end duration duration-unit bar-size bar-size-unit :trades true)))

  (request-real-time-bars
    [this id contract what-to-show use-regular-trading-hours?]
    (.reqRealTimeBars this id contract 5
                      (translate :to-ib :what-to-show what-to-show)
                      use-regular-trading-hours?))

  MarketNewsProvider
  (request-news-bulletins
    ([this]
       (.reqNewsBulletins this true))
    ([this all-messages?]
       (.reqNewsBulletins this all-messages?)))

  (cancel-news-bulletins
    [this]
    (.cancelNewsBulletins this))

  SecurityDataProvider
  (request-fundamental-data
    ([this contract report-type]
       (.reqFundamentalData this (get-request-id) (map-> com.ib.client.Contract contract)
                            (translate :to-ib :report-type report-type)))
    ([this request-id contract report-type]
       (.reqFundamentalData this request-id (map-> com.ib.client.Contract contract)
                            (translate :to-ib :report-type report-type))))

  (cancel-fundamental-data
    [this request-id]
    (.cancelFundamentalData this request-id))

  (request-contract-details
    ([this contract]
       (request-contract-details this (get-request-id) contract))
    ([this request-id contract]
       (.reqContractDetails this request-id (map-> com.ib.client.Contract contract))
       request-id))

  OrderManager
  (place-order
    ([this contract order]
       (let [order-id (get-order-id)]
         (place-order this order-id contract (assoc order :order-id order-id))))
    ([this order-id contract order]
       (.placeOrder this order-id (map-> com.ib.client.Contract contract) (map-> com.ib.client.Order order))))

  (cancel-order
    [this order-id]
    (.cancelOrder this order-id))

  (request-open-orders
    [this]
    (.reqOpenOrders this))

  (request-executions
    ([this]
       (.reqExecutions this nil))
    ([this client-id]
       (.reqExecutions this client-id)))

  AccountDataProvider
  (request-account-updates
    [this subscribe? account-code]
    (.reqAccountUpdates this subscribe? account-code)))

(defn connect
  "This function must be called before any other. There is no feedback
   for a successful connection, but a subsequent attempt to connect
   will return the message 'Already connected.'

   wrapper is an implementation of the EWrapper interface.

   host is the hostname running IB Gateway or TWS.

   port is the port IB Gateway / TWS is running on.

   client-id identifies this client. Only one connection to a gateway can
   be made per client-id at a time."
  ([handler-fn] (connect handler-fn "localhost" 7496))
  ([handler-fn client-id] (connect handler-fn "localhost" 7496 client-id))
  ([handler-fn host port] (connect handler-fn host port *client-id*))
  ([handler-fn host port client-id]
     (let [wrapper (create-wrapper handler-fn)
           connection (com.ib.client.EClientSocket. wrapper)]
       (.eConnect connection host port client-id)
       (if (not= *default-server-log-level* :error)
         (set-server-log-level connection *default-server-log-level*))
       connection)))

(defmacro with-open-connection [bindings & body]
  `(let ~(subvec bindings 0 2)
     (try
       ~@body
       (finally
        (disconnect ~(bindings 0))))))

(defn collect-messages [start end action]
  (let [req-id (.. (java.util.Random.) nextInt)
        results (promise)
        accum (atom [])
        handler (fn [{:keys [type request-id] :as msg}]
                  (when *prn-raw-messages* (prn "Raw: " msg))
                  (if (= req-id request-id)
                    (cond
                     (= type start) (swap! accum conj msg)
                     (= type end) (deliver results @accum)
                     (= type :error) (if (error? msg) (deliver results msg)))))
        connection (connect handler)]
    (action connection req-id)
    @results))