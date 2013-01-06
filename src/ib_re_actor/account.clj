;; # Account details
;; This namespace deals monitoring and updating account details.
;;
(ns ib-re-actor.account
  (:require [ib-re-actor.gateway :as g]))

;; This atom contains the account details at all times
(defonce account-details (atom nil))

(defn update-account-details
  "Update a partical key in the account details"
  [{:keys [type key value currency]}]
  (case type
    :update-account-value
    (swap! account-details assoc key
           (if (nil? currency)
             value
             (vector value currency)))

    :update-account-time
    (swap! account-details assoc :last-updated value)))

;; Subscribe immediately to receiving updates
(do
  (g/subscribe update-account-details))
