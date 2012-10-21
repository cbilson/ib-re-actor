(ns ib-re-actor.examples.historical-data-request
  (:require [clojure.string :as s]
            [ib-re-actor.gateway :as g])
  (:use [ib-re-actor.synchronous :only [get-historical-data]]
        [clj-time.core :only [date-time minus plus hours minutes
                              year month day before?]]
        [clojure.tools.logging :only [error debug]]))

(defn end-times [start end t]
  (letfn [(add-t [x] (plus x t))]
    (->> (iterate add-t start)
         (take-while #(before? % end))
         (map add-t))))

(defn midnight-on [d]
  (date-time (year d) (month d) (day d)))

(defn get-daily-data
  [contract date]
  (let [req-id (g/get-request-id)
        midnight (midnight-on date)
        end (plus midnight (hours 20) (minutes 15))
        start (plus midnight (hours 11) (minutes 30))
        end-times (agent (end-times start end (minutes 30)))
        all-complete (promise)
        make-request (fn [[end-time & next-end-times]]
                       (if end-time
                         (do
                           (debug "Requesting up to " end-time)
                           (g/request-historical-data req-id contract end-time
                                                      30 :minutes 1 :second
                                                      :trades false))
                         (do
                           (debug "all done")
                           (deliver all-complete true)))
                       next-end-times)
        send-request (fn []
                       (debug "Delaying request 15 seconds...")
                       (future
                         (do (Thread/sleep 15000)
                             (debug "delay over")
                             (send end-times make-request))))
        handler (fn [{:keys [type request-id] :as msg}]
                  (when (= req-id request-id)
                    (case type
                      :price-bar
                      (->> (map msg [:time :open :high :low :close
                                     :volume :trade-count :has-gaps?])
                           (s/join ",")
                           println)

                      :price-bar-complete
                      (send-request)

                      :otherwise
                      (debug msg))))]
    (g/subscribe handler)
    (try
      (send end-times make-request)
      @all-complete
      (finally
        (g/unsubscribe handler)))))

(comment
  (require 'clj-logging-config.log4j)
  (clj-logging-config.log4j/set-logger! :level :debug))