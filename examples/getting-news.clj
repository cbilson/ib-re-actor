(ns ib-re-actor.examples.getting-news
  (:use [ib-re-actor.connection]))

(def news-complete? (atom false))
(defmulti news-handler :type)
(defmethod news-handler :error [msg]
  (cond
   (contains? msg :message) (println "*** " (:message msg))
   (contains? msg :exception) (println "*** " (.toString (:exception msg))))
  (if (not (warning? msg))
    (reset! news-complete? true)))
(defmethod news-handler :default [msg] (prn msg))
(defmethod news-handler :update-news-bulletin [{message :message}]
  (println message))

(defn watch-news []
  (let [connection (connect news-handler)]
    (try
      (reset! news-complete? false)
      (request-news-bulletins connection)
      (while (not @news-complete?)
        (java.lang.Thread/sleep 100))
      (finally (disconnect connection)))))

