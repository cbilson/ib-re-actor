(ns ib-re-actor.examples.getting-fundamentals
  (:use [ib-re-actor.connection]
        [ib-re-actor.contracts]
        [clj-time.core :only [date-time minus]]))

(def fundamental-data-complete? (atom false))
(defmulti fundamental-data-handler :type)
(defmethod fundamental-data-handler :error [msg]
  (cond
   (contains? msg :message) (println "*** " (:message msg))
   (contains? msg :exception) (println "*** " (.toString (:exception msg))))
  (if (not (warning? msg))
    (reset! fundamental-data-complete? true)))
(defmethod fundamental-data-handler :default [msg] (prn msg))
(defmethod fundamental-data-handler :fundamental-data [{report :report}]
  (prn report)
  (reset! fundamental-data-complete? true))

(defn get-fundamentals []
  (let [connection (connect fundamental-data-handler)]
    (try
      (reset! fundamental-data-complete? false)
      (request-fundamental-data connection -1 (equity "C" "NYSE") :summary)
      (while (not @fundamental-data-complete?)
        (java.lang.Thread/sleep 100))
      (finally (disconnect connection)))))
