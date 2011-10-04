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
      (process-message {:ticker-id tickerId
                        :field (translate-from-ib-tick-field field)
                        :price price
                        :can-auto-execute? (= 1 canAutoExecute)}))

    ;; (error [this request-id error-code message]
    ;; (.error handler {:request-id request-id
    ;;                  :error-code error-code
    ;;                  :message message})
    ;;   )
    ;; (^void error [this ^String message]
    ;;   ;; (.error handler {:message message})
    ;;   )
    ;; (^void error [this ^Exception ex]
    ;;   ;; (.error handler {:message (.toString ex) :exception ex})
    ;;   )
    ;; (currentTime [this time]
    ;;   ;; (.error handler {:server-date (convert-ib-date-time time)
    ;;   ;;                  :local-date (now)})
    ;;   )
    ;; (tickSnapshotEnd [this reqId])
    ;; (connectionClosed [this]
    ;;   ;; (.closed handler)
    ;;   )
    ;; (tickSize [this tickerId field size])
    ;; (tickOptionComputation [this tickerId field impliedVol delta optPrice pvDividend gamma vega theta undPrice])
    ;; (tickGeneric [this tickerId tickType value])
    ;; (tickString [this tickerId tickType value])
    ;; (tickEFP [this tickerId tickType basisPoints formattedBasisPoints impliedFuture holdDays futureExpiry dividendImpact dividendsToExpiry])
    ;; (orderStatus [this orderId status filled remaining avgFillPrice permId parentId lastFillPrice clientId whyHeld])
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


