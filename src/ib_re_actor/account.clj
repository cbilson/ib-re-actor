(ns ib-re-actor.account
  (:require [ib-re-actor.gateway :as g]))

(defonce account-details (atom nil))

(defn update-account-details [{:keys [type key value currency]}]
  (case type
    :update-account-value
    (swap! account-details assoc key
           (if (nil? currency)
             value
             (vector value currency)))

    :update-account-time
    (swap! account-details assoc :last-updated value)))

(do
  (g/subscribe update-account-details))