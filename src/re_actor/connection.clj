(ns re-actor.connection
  (:use [re-actor.conversions])
  (:import [com.ib.client EClientSocket EWrapper]))

(defrecord PriceBar [request-id time open high low close volume count WAP has-gaps?])
(defrecord Complete [request-id])
(defrecord Tick [ticker-id field value can-auto-execute?])

(defrecord OrderStatus [order-id status filled remaining average-fill-price
                        permanent-id parent-id last-fill-price client-id
                        why-held])

(defrecord Position [contract quantity market-price market-value average-cost unrealized-gain-loss realized-gain-loss account-name])

(defn- is-finish? [date-string]
  (.startsWith date-string "finished"))

(defn create-client [process-message]
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
      (process-message {:type :string-tick :field (translate-from-ib-tick-field-code tickType)
                        :ticker-id tickerId :value value}))
    
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
    
    ;; (openOrder [this orderId contract order orderState])
    ;; (openOrderEnd [this])
    ;; (nextValidId [this orderId])
    ;; (updateAccountValue [this key value currency accountName])
    ;; (updatePortfolio [this contract position marketPrice marketValue averageCost unrealizedPNL realizedPNL accountName])
    ;; (updateAccountTime [this timeStamp])
    ;; (contractDetails [this request-id contractDetails]
    ;;   ;; (.contract-details handler request-id
    ;;   ;;                    {:summary (.m_summary contractDetails)
    ;;   ;;                     :market-name (.m_marketName contractDetails)
    ;;   ;;                     :trading-class (.m_tradingClass contractDetails)
    ;;   ;;                     :min-tick (.m_minTick contractDetails)
    ;;   ;;                     :price-magnifier (.m_priceMagnifier contractDetails)
    ;;   ;;                     :order-types (.m_orderTypes contractDetails)
    ;;   ;;                     :valid-exchanges (.m_validExchanges contractDetails)
    ;;   ;;                     :underlying-contract-id (.m_underConId contractDetails)
    ;;   ;;                     :long-name (.m_longName contractDetails)
    ;;   ;;                     :cusip (.m_cusip contractDetails)
    ;;   ;;                     :ratings (.m_ratings contractDetails)
    ;;   ;;                     :description (.m_descAppend contractDetails)
    ;;   ;;                     :bond-type (.m_bondType contractDetails)
    ;;   ;;                     :coupon-type (.m_couponType contractDetails)
    ;;   ;;                     :callable (.m_callable contractDetails)
    ;;   ;;                     :putable (.m_putable contractDetails)
    ;;   ;;                     :coupon (.m_coupon contractDetails)
    ;;   ;;                     :convertible (.m_convertible contractDetails)
    ;;   ;;                     :maturity (.m_maturity contractDetails)
    ;;   ;;                     :issue-date (.m_issueDate contractDetails)
    ;;   ;;                     :next-option-date (.m_nextOptionDate contractDetails)
    ;;   ;;                     :next-option-type (.m_nextOptionType contractDetails)
    ;;   ;;                     :next-option-partial (.m_nextOptionPartial contractDetails)
    ;;   ;;                     :notes (.m_notes contractDetails)
    ;;   ;;                     :contract-month (.m_contractMonth contractDetails)
    ;;   ;;                     :industry (.m_industry contractDetails)
    ;;   ;;                     :category (.m_category contractDetails)
    ;;   ;;                     :subcategory (.m_subcategory contractDetails)
    ;;   ;;                     :time-zone-id (.m_timeZoneId contractDetails)
    ;;   ;;                     :trading-hours (.m_tradingHours contractDetails)
    ;;   ;;                     :liquid-hours (.m_liquidHours contractDetails)})
    ;;   )
    ;; (contractDetailsEnd [this request-id]
    ;;   ;; (.contract-details-end handler request-id)
    ;;   )
    ;; (bondContractDetails [this request-id contractDetails])
    ;; (execDetails [this request-id contract execution])
    ;; (execDetailsEnd [this request-id])
    ;; (updateMktDepth [this tickerId position operation side price size])
    ;; (updateMktDepthL2 [this tickerId position marketMaker operation side price size])
    ;; (updateNewsBulletin [this msgId msgType message origExchange])
    ;; (managedAccounts [this accountsList])
    ;; (receiveFA [this faDataType xml])
    ;; (scannerParameters [this xml])
    ;; (scannerData [this request-id rank contractDetails distance benchmark projection legsStr])
    ;; (scannerDataEnd [this request-id])
    ))

;; (defn connect
;;   "This function must be called before any other. There is no feedback
;; for a successful connection, but a subsequent attempt to connect
;; will return the message 'Already connected.'

;; wrapper is an implementation of the EWrapper interface.

;; host is the hostname running IB Gateway or TWS.

;; port is the port IB Gateway / TWS is running on.

;; client-id identifies this client. Only one connection to a gateway can
;; be made per client-id at a time."
;;   ([wrapper] (connect wrapper "localhost"))
;;   ([wrapper host] (connect wrapper host 7496))
;;   ([wrapper host port] (connect wrapper host port 1))
;;   ([wrapper host port client-id]
;;      (let [connection (EClientSocket. wrapper)]
;;        (doto connection
;;          (.eConnect host port client-id)))))


