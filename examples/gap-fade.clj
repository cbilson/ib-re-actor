(require '[re-actor.connection :only [connection]]
         '[re-actor.contracts])

(def target-contract (future-contract "YM" "GLOBEX" (date-time 2011 12)))

(def pivots (atom {}))

(defmulti calculate-pivots-fn :type)
(defmulti calculate-pivots-fn :price-bar [msg])
(defmulti calculate-pivots-fn :complete [msg])

(defn calculate-pivots []
)
