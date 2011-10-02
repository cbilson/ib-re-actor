(ns re-actor.historic-data)

(defprotocol HistoricalReceiver
  (bar [this request-id bar])
  (complete [this request-id]))

(defrecord NullHistoricalReceiver []
    HistoricalReceiver
  (bar [_ _ _])
  (complete [_ _]))




