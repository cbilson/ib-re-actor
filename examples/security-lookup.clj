(ns ib-re-actor.examples.security-lookup
  (:use [ib-re-actor.securities]
        [clj-time.core :only [date-time]]
        [clojure.pprint :only [pprint]]))

(defn print-contracts-details [contract-details]
  (pprint contract-details))

(-> (lookup-security {:security-type :future :symbol "ES" :exchange "GLOBEX"})
    print-contracts-details)