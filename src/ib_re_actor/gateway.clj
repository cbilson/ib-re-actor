(ns ib-re-actor.gateway
     "Functions for connecting to Interactive Brokers TWS Gateway and sending requests to it."
     (:use [ib-re-actor.translation :only [translate integer-account-value? numeric-account-value? boolean-account-value?]]
           [ib-re-actor.mapping])
     (:require [clojure.xml :as xml]
               [clojure.tools.logging :as log]))

(defn- get-stack-trace [ex]
  (let [sw (java.io.StringWriter.)
        pw (java.io.PrintWriter. sw)]
    (.printStackTrace ex pw)
    (.toString sw)))

(defn- log-exception
  ([ex msg]
     (error msg ": " (.getMessage ex))
     (error "Stack Trace: " (get-stack-trace ex)))
  ([ex]
     (log-exception "Error" ex)))

(defonce client-id (atom 100))
(defonce next-order-id (atom 0))
(defonce next-request-id (atom 0))
(defonce default-server-log-level :error)
(defonce contract->id (atom {}))
(defonce id->contract (atom {}))
(defonce next-ticker-id (atom 1))

(defn register-contract [contract]
  (let [existing (get @contract->id contract)]
    (or existing
        (let [id (swap! next-ticker-id inc)]
          (swap! contract->id assoc contract id)
          (swap! id->contract assoc id contract)
          id))))

(defonce connection (agent nil))

(set-error-mode! connection :continue)
(set-error-handler! connection
                    (fn [a ex]
                      (log-exception "Connection Error" ex)
                      (try (.eDisconnect @a)
                           (catch Exception ex
                             (log-exception "Error disconnecting" ex)))))

(defonce listeners (agent nil))
(set-error-mode! listeners :continue)
(set-error-handler! listeners
                    (fn [a ex]
                      (log-exception "Error in handler" ex)))

(defmacro send-connection [f & args]
  `(send-off connection
             (fn [c#]
               (~f c# ~@args)
               c#)))

(defn- invoke-all [fs message]
  (doseq [f fs]
    (try
      (f message)
      (catch Exception ex (log-exception ex))))
  fs)

(defn- dispatch-message [msg]
  (log/debug "Dispatching: " msg)
  (send-off listeners invoke-all msg))

(defn- is-finish? [date-string]
  (.startsWith date-string "finished"))

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

(defn is-end?
  "Predicate to determine if a message indicates a tick snapshot is done"
  [{type :type :as msg}]
  (cond
   (= :tick-snapshot-end type) true
   (and (= :error type) (error? msg)) true
   :otherwise false))

(defn- create-wrapper
  "Creates a wrapper that flattens the Interactive Brokers EWrapper interface,
   calling a single function with maps that all have a :type to indicate what type
   of messages was received, and the massaged parameters from the event."
  []
  (reify
    com.ib.client.EWrapper

    ;;; Connection and Server
    (currentTime [this time]
      (dispatch-message {:type :current-time :value (translate :from-ib :date-time time)}))

    (error [this requestId errorCode message]
      (dispatch-message {:type :error :request-id requestId :code errorCode :message message}))

    (^void error [this ^String message]
      (dispatch-message {:type :error :message message}))

    (^void error [this ^Exception ex]
      (let [sw (java.io.StringWriter.)
            pw (java.io.PrintWriter. sw)]
        (.printStackTrace ex pw)
        (error "Error: " (.getMessage ex))
        (error "Stack Trace: " sw))
      (dispatch-message {:type :error :exception (.toString ex)}))

    (connectionClosed [this]
      (dispatch-message {:type :connection-closed})
      (restart-agent connection nil))

    ;;; Market Data
    (tickPrice [this tickerId field price canAutoExecute]
      (dispatch-message {:type :tick :field (translate :from-ib :tick-field-code field)
                         :contract (get @id->contract tickerId)
                         :value price
                         :can-auto-execute? (= 1 canAutoExecute)}))

    (tickSize [this tickerId field size]
      (dispatch-message {:type :tick :field (translate :from-ib :tick-field-code field)
                         :contract (get @id->contract tickerId)
                         :value size}))

    (tickOptionComputation [this tickerId field impliedVol delta optPrice pvDividend gamma vega theta undPrice]
      (dispatch-message {:type :tick
                         :field (translate :from-ib :tick-field-code field)
                         :contract (get @id->contract tickerId)
                         :implied-volatility impliedVol
                         :option-price optPrice
                         :pv-dividends pvDividend
                         :underlying-price undPrice
                         :delta delta :gamma gamma :theta theta :vega vega}))

    (tickGeneric [this tickerId tickType value]
      (dispatch-message {:type :tick
                         :field (translate :from-ib :tick-field-code tickType)
                         :contract (get @id->contract tickerId) :value value}))

    (tickString [this tickerId tickType value]
      (let [field (translate :from-ib :tick-field-code tickType)]
        (cond
         (= field :last-timestamp)
         (dispatch-message {:type :timestamp-tick :field field
                            :contract (get @id->contract tickerId)
                            :value (translate :from-ib :date-time value)})

         :else
         (dispatch-message {:type :string-tick :field field
                            :contract (get @id->contract tickerId)
                            :value val}))))

    (tickEFP [this tickerId tickType basisPoints formattedBasisPoints impliedFuture holdDays futureExpiry dividendImpact dividendsToExpiry]
      (dispatch-message {:type :tick
                         :field (translate :from-ib :tick-field-code tickType)
                         :contract (get @id->contract tickerId)
                         :basis-points basisPoints :formatted-basis-points formattedBasisPoints
                         :implied-future impliedFuture :hold-days holdDays :future-expiry futureExpiry
                         :dividend-impact dividendImpact :dividends-to-expiry dividendsToExpiry}))

    (tickSnapshotEnd [this reqId]
      (dispatch-message {:type :tick-snapshot-end :request-id reqId}))

    (marketDataType [this reqId type]
      (dispatch-message {:type :market-data-type
                         :request-id reqId
                         :market-data-type (translate :from-ib :market-data-type type)}))

    ;;; Orders
    (orderStatus [this orderId status filled remaining avgFillPrice permId parentId lastFillPrice clientId whyHeld
                  ]
      (dispatch-message {:type :order-status :order-id orderId
                         :status (translate :from-ib :order-status status)
                         :filled filled :remaining remaining
                         :average-fill-price avgFillPrice
                         :permanent-id permId :parent-id parentId
                         :last-fill-price lastFillPrice :client-id clientId
                         :why-held whyHeld}))

    (openOrder [this orderId contract order orderState]
      (dispatch-message {:type :open-order :order-id orderId :contract (->map contract)
                         :order (->map order) :order-state (->map orderState)}))

    (openOrderEnd [this]
      (dispatch-message {:type :open-order-end}))

    (nextValidId [this orderId]
      (dosync
       (reset! next-order-id orderId))
      (dispatch-message {:type :next-valid-order-id :value orderId}))

    ;;; Account and Portfolio
    (updateAccountValue [this key value currency accountName]
      (let [avk (translate :from-ib :account-value-key key)
            val (cond
                 (integer-account-value? avk) (Integer/parseInt value)
                 (numeric-account-value? avk) (Double/parseDouble value)
                 (boolean-account-value? avk) (Boolean/parseBoolean value)
                 :else value)]
        (dispatch-message {:type :update-account-value :key avk :value val :currency currency :account accountName})))

    (accountDownloadEnd [this account-code]
      (dispatch-message {:type :account-download-end :account-code account-code}))

    (updatePortfolio [this contract position marketPrice marketValue averageCost unrealizedPNL realizedPNL accountName]
      (dispatch-message {:type :update-portfolio :contract (->map contract) :position position
                         :market-price marketPrice :market-value marketValue
                         :average-cost averageCost :unrealized-gain-loss unrealizedPNL
                         :realized-gain-loss realizedPNL
                         :account accountName}))

    (updateAccountTime [this timeStamp]
      (dispatch-message {:type :update-account-time
                         :value (translate :from-ib :time-of-day timeStamp)}))

    ;;; Contract Details
    (contractDetails [this requestId contractDetails]
      (dispatch-message {:type :contract-details :request-id requestId
                         :value (->map contractDetails)}))

    (bondContractDetails [this requestId contractDetails]
      (dispatch-message {:type :contract-details :request-id requestId :value contractDetails}))

    (contractDetailsEnd [this requestId]
      (dispatch-message {:type :contract-details-end :request-id requestId}))

    ;;; Execution Details
    (execDetails [this requestId contract execution]
      (dispatch-message {:type :execution-details :request-id requestId :contract (->map contract) :value execution}))

    (execDetailsEnd [this requestId]
      (dispatch-message {:type :execution-details-end :request-id requestId}))

    (commissionReport [this commissionReport]
      (dispatch-message {:type :commission-report :report (->map commissionReport)}))

    ;;; Market Depth
    (updateMktDepth [this tickerId position operation side price size]
      (dispatch-message {:type :update-market-depth
                         :contract (get @id->contract tickerId)
                         :position position
                         :operation (translate :from-ib :market-depth-row-operation operation)
                         :side (translate :from-ib :market-depth-side side)
                         :price price :size size}))

    (updateMktDepthL2 [this tickerId position marketMaker operation side price size]
      (dispatch-message {:type :update-market-depth-level-2
                         :contract (get @id->contract tickerId) :position position
                         :market-maker marketMaker
                         :operation (translate :from-ib :market-depth-row-operation operation)
                         :side (translate :from-ib :market-depth-side side)
                         :price price :size size}))

    ;;; News Bulletin
    (updateNewsBulletin [this msgId msgType message origExchange]
      (dispatch-message {:type (condp = msgType
                                 0 :news-bulletin
                                 1 :exchange-unavailable
                                 2 :exchange-available)
                         :id msgId :message message :exchange origExchange}))

    ;;; Financial Advisors
    (managedAccounts [this accountsList]
      (dispatch-message {:type :managed-accounts
                         :accounts (->> (.split accountsList ",") (map #(.trim %)) vec)}))

    (receiveFA [this faDataType xml]
      (dispatch-message {:type (condp = faDataType
                                 1 :financial-advisor-groups
                                 2 :financial-advisor-profile
                                 3 :financial-advisor-account-aliases)
                         :value xml}))

    ;;; Historical Data
    (historicalData [this requestId date open high low close volume count wap hasGaps]
      (if (is-finish? date)
        (dispatch-message {:type :price-bar-complete :request-id requestId})
        (do (log/info date)
            (dispatch-message
             {:type :price-bar :request-id requestId
              :time (translate :from-ib :timestamp date)
              :open open :high high :low low :close close :volume volume
              :trade-count count :WAP wap :has-gaps? hasGaps}))))

    ;;; Market Scanners
    (scannerParameters [this xml]
      (dispatch-message {:type :scan-parameters :value xml}))

    (scannerData [this requestId rank contractDetails distance benchmark projection legsStr]
      (dispatch-message {:type :scan-result :request-id requestId :rank rank
                         :contract-details (->map contractDetails) :distance distance
                         :benchmark benchmark :projection projection
                         :legs legsStr}))

    (scannerDataEnd [this requestId]
      (dispatch-message {:type :scan-end :request-id requestId}))

    ;;; Real Time Bars
    (realtimeBar [this requestId time open high low close volume wap count]
      (dispatch-message {:type :price-bar :request-id requestId
                         :time (translate :from-ib :date-time time)
                         :open open :high high :low low :close close :volume volume
                         :count count :WAP wap}))

    ;;; Fundamental Data
    (fundamentalData [this requestId xml]
      (dispatch-message {:type :fundamental-data :request-id requestId
                         :report (xml/parse (java.io.ByteArrayInputStream. (.getBytes xml)))}))))

(defn subscribe [f]
  (send-off listeners conj f))

(defn unsubscribe [f]
  (send-off listeners
            (fn [fns] (filter (partial not= f) fns))))

(defn get-order-id []
  (swap! next-order-id inc))

(defn get-request-id []
  (swap! next-request-id inc))

(defn request-market-data
  "Call this function to request market data. The market data will be returned by
   :price-tick, :size-tick, :option-computation-tick, :generic-tick, :string-tick
   and :efp-tick messages.

   For snapshots, a :tick-snapshot-end message will indicate the snapshot is done.

   ## Parameters
   - this
     The connection to use to make the request. Use (connect) to get this.

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
     and have the market data subscription will be immediately canceled."
  ([contract tick-list snapshot?]
     (let [ticker-id (register-contract contract)]
       (send-connection .reqMktData ticker-id
                        (map-> com.ib.client.Contract contract)
                        (translate :to-ib :tick-list tick-list)
                        snapshot?)))
  ([contract]
     (request-market-data contract "" false)))

(defn cancel-market-data [contract]
  (let [ticker-id (@contract->id contract)]
    (log/info "cancelling " ticker-id)
    (send-connection .cancelMktData ticker-id)))

(defn request-historical-data
  "Start receiving historical price bars stretching back <duration> <duration-unit>s back,
   up till <end> for the specified contract. The messages will have :request-id of <id>.

   duration-unit should be one of :second(s), :day(s), :week(s), or :year(s).

   bar-size-unit should be one of :second(s), :minute(s), :hour(s), or :day(s).

   what-to-show should be one of :trades, :midpoint, :bid, :ask, :bid-ask, :historical-volatility,
   :option-implied-volatility, :option-volume, or :option-open-interest."
  ([id contract end duration duration-unit bar-size bar-size-unit what-to-show use-regular-trading-hours?]
     (let [[acceptable-duration acceptable-duration-unit] (translate :to-ib :acceptable-duration [duration duration-unit])]
       (send-connection .reqHistoricalData id
                        (map-> com.ib.client.Contract contract)
                        (translate :to-ib :date-time end)
                        (translate :to-ib :duration [acceptable-duration acceptable-duration-unit])
                        (translate :to-ib :bar-size [bar-size bar-size-unit])
                        (translate :to-ib :what-to-show what-to-show)
                        (if use-regular-trading-hours? 1 0)
                        2)))
  ([id contract end duration duration-unit bar-size bar-size-unit what-to-show]
     (request-historical-data id contract end duration duration-unit bar-size bar-size-unit what-to-show true))
  ([id contract end duration duration-unit bar-size bar-size-unit]
     (request-historical-data id contract end duration duration-unit bar-size bar-size-unit :trades true)))

(defn request-real-time-bars
  "Start receiving real time bar results."
  [id contract what-to-show use-regular-trading-hours?]
  (send-connection .reqRealTimeBars id contract 5
                   (translate :to-ib :what-to-show what-to-show)
                   use-regular-trading-hours?))

(defn request-news-bulletins
  "Call this function to start receiving news bulletins. Each bulletin will
   be sent in a :news-bulletin, :exchange-unavailable, or :exchange-available
   message."
  ([]
     (request-news-bulletins true))
  ([all-messages?]
     (send-connection .reqNewsBulletins all-messages?)))

(defn cancel-news-bulletins
  "Call this function to stop receiving news bulletins."
  []
  (send-connection .cancelNewsBulletins))

(defn request-fundamental-data
  "Call this function to receive Reuters global fundamental data. There must be a
   subscription to Reuters Fundamental set up in Account Management before you
   can receive this data."
  ([contract report-type]
     (request-fundamental-data (get-request-id) contract report-type))
  ([request-id contract report-type]
     (send-connection .reqFundamentalData request-id (map-> com.ib.client.Contract contract)
                      (translate :to-ib :report-type report-type))))

(defn cancel-fundamental-data
  "Call this function to stop receiving Reuters global fundamental data."
  [request-id]
  (send-connection .cancelFundamentalData request-id))

(defn request-contract-details
  "Call this function to download all details for a particular
contract. The contract details will be received in a :contract-details
message"
  ([contract]
     (request-contract-details (get-request-id) contract))
  ([request-id contract]
     (log/info "Requesting contract details #" request-id " for " (pr-str contract))
     (send-connection .reqContractDetails request-id (map-> com.ib.client.Contract contract))
     request-id))

(defn place-order
  ([contract order]
     (let [order-id (get-order-id)]
       (place-order order-id contract (assoc order :order-id order-id))))
  ([order-id contract order]
     (send-connection .placeOrder order-id
                      (map-> com.ib.client.Contract contract)
                      (map-> com.ib.client.Order order))))

(defn cancel-order
  [order-id]
  (send-connection .cancelOrder order-id))

(defn request-open-orders []
  (send-connection .reqOpenOrders))

(defn request-executions
  ([]
     (request-executions nil))
  ([client-id]
     (send-connection .reqExecutions client-id)))

(defn request-account-updates
  [subscribe? account-code]
  (send-connection .reqAccountUpdates subscribe? account-code))

(defn set-server-log-level
  "Call this function to set the log level used on the server."
  [this log-level]
  (send-connection .setServerLogLevel (translate :to-ib :log-level log-level)))

(defn connect
  "This function must be called before any other. There is no feedback
   for a successful connection, but a subsequent attempt to connect
   will return the message 'Already connected.'

   wrapper is an implementation of the EWrapper interface.

   host is the hostname running IB Gateway or TWS.

   port is the port IB Gateway / TWS is running on.

   client-id identifies this client. Only one connection to a gateway can
   be made per client-id at a time."
  ([] (connect "localhost" 7496))
  ([client-id] (connect "localhost" 7496 client-id))
  ([host port] (connect host port @client-id))
  ([host port client-id]
     (send-off connection
               (fn [c]
                 (try
                   (when (and c (.isConnected c))
                     (.eDisconnect c))

                   (let [connection (com.ib.client.EClientSocket. (create-wrapper))]
                        (.eConnect connection host port client-id)
                        (if (not= default-server-log-level :error)
                          (set-server-log-level connection default-server-log-level))
                        connection)
                   (catch Exception ex
                     (error "Error trying to connect to " host ":" port ": " ex)))))))

(defn disconnect
  "Call this function to terminate the connections with TWS.
   Calling this function does not cancel orders that have already been sent."
  []
  (send-off connection (fn [c] (.eDisconnect c) nil)))

(defn request-current-time []
  (send-connection .reqCurrentTime))

(defn connection-time []
  (->> (.TwsConnectionTime @connection)
       (translate :from-ib :connection-time)))

(defn request-server-version []
  (send-connection .serverVersion))
