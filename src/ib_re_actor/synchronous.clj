(ns ib-re-actor.synchronous
  "Functions for doing things with interactive brokers in a synchronous manner.
   Mostly these are just wrappers that create a temporary handler function,
   wait for a response, then unsubscribe. These are much easier to use in an
   interactive context (such as when using the REPL) but probably not what you
   would want to use in an application, as the asynchronous API is a much more
   natural fit for building programs that react to events in market data."
  (:require [ib-re-actor.gateway :as g]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]))

(defn- reduce-responses [accept? reduce-fn done? completion-fn subscribe-fn & args]
  "Given some functions, subscribes to the message stream, reducing each message into
   an accumulator, then cleans up when done."
  (let [responses (atom nil)
        result (promise)
        handler (fn [msg]
                  (when (if (fn? accept?)
                          (accept? msg)
                          true)
                    (if (g/is-end? msg)
                      (deliver result msg)
                      (swap! responses reduce-fn msg))))
        response-watch (fn [_ k _ v]
                         (when (done? v)
                           (remove-watch responses k)
                           (deliver result v)))]
    (add-watch responses :result-watch response-watch)
    (g/subscribe handler)
    (try
      (apply subscribe-fn args)
      @result
      (finally
        (log/debug "unsubscribing")
        (g/unsubscribe handler)
        (log/debug "completing")
        (if (fn? completion-fn)
          (completion-fn))
        (log/debug "completed")))))

(defn get-time
  "Returns the server time"
  []
  (reduce-responses #(= :current-time (:type %)) conj (comp not empty?) nil g/request-current-time))

(defn get-contract-details
  "Gets details for the specified contract.

Example:
user> (get-contract-details {:type :future :exchange \"NYBOT\" :local-symbol \"TFZ2\"})
({:type :contract-details, :request-id 18, :value {:next-option-partial false, :time-zone-id \"EST\", :underlying-contract-id 54421265, :price-magnifier 1, :trading-hours \"20121018:2000-1800;20121019:2000-1800\", :long-name \"Russell 2000 Mini Futures\", :convertible? false, :liquid-hours \"20121018:0000-1800,2000-2359;20121019:0000-1800\", :callable? false, :order-types (:ACTIVETIM :ADJUST :ALERT :ALGO :ALLOC :average-cost :basket :COND :CONDORDER :DAY :DEACT :DEACTDIS :DEACTEOD :good-after-time :good-till-canceled :good-till-date :GTT :HID :ICE :limit-if-touched :limit :market-if-touched :market :market-to-limit :NONALGO :one-cancels-all :scale :SCALERST :stop :stop-limit :trail :trailing-limit-if-touched :trailing-stop-limit :trailing-market-if-touched :what-if), :valid-exchanges [\"MIBSX\" \"NYBOT\"], :min-tick 0.1, :contract-month \"201212\", :trading-class \"TF\", :putable? false, :summary {:multiplier 100.0, :expiry #<DateTime 2012-12-21T00:00:00.000Z>, :include-expired? false, :type :future, :currency \"USD\", :local-symbol \"TFZ2\", :exchange \"NYBOT\", :symbol \"TF\", :contract-id 99292540}, :market-name \"TF\", :coupon 0.0}})"
                                                   [contract]
                                                   (let [req-id (g/get-request-id)]
                                                     (reduce-responses #(= req-id (:request-id %)) conj (comp not empty?) nil g/request-contract-details req-id contract)))

  (defn get-current-price
    "Gets the current price for the specified contract.

Example:
user> (get-current-price {:type :equity :symbol \"IBKR\")
({:type :contract-details, :request-id 33, :value {:next-option-partial false, :time-zone-id \"EST\", :underlying-contract-id 0, :price-magnifier 1, :industry \"Financial\", :trading-hours \"20121018:0400-2000;20121019:0400-2000\", :long-name \"INTERACTIVE BROKERS GRO-CL A\", :convertible? false, :subcategory \"Finance-Invest Bnkr/Brkr\", :liquid-hours \"20121018:0930-1600;20121019:0930-1600\", :callable? false, :order-types (:ACTIVETIM :ADJUST :ALERT :ALGO :ALLOC :AON :average-cost :basket :COND :CONDORDER :DARKPOLL :DAY :DEACT :DEACTDIS :DEACTEOD :DIS :good-after-time :good-till-canceled :good-till-date :GTT :HID :ICE :IMB :immediate-or-cancel :limit-if-touched :limit :limit-on-close :market-if-touched :market :market-on-close :market-to-limit :NONALGO :one-cancels-all :OPG :OPGREROUT :POSTONLY :PREOPGRTH :relative :RTH :scale :SCALERST :stop :stop-limit :SWEEP :TIMEPRIO :trail :trailing-limit-if-touched :trailing-stop-limit :trailing-market-if-touched :what-if), :valid-exchanges [\"SMART\" \"ARCA\" \"BATS\" \"BEX\" \"BTRADE\" \"BYX\" \"CBSX\" \"CHX\" \"CSFBALGO\" \"DRCTEDGE\" \"EDGEA\" \"IBSX\" \"ISE\" \"ISLAND\" \"JEFFALGO\" \"LAVA\" \"MIBSX\" \"NSX\" \"PSX\"], :min-tick 0.01, :trading-class \"NMS\", :putable? false, :summary {:include-expired? false, :type :equity, :currency \"USD\", :primary-exchange \"NASDAQ\", :local-symbol \"IBKR\", :exchange \"SMART\", :symbol \"IBKR\", :contract-id 43645865}, :market-name \"NMS\", :coupon 0.0, :category \"Diversified Finan Serv\"}}) "
                                                   [con]
                                                   (let [fields [:open-tick :bid-price :close-price :last-size :low :ask-size :bid-size :last-price :ask-price :high :volume]
                                                         accept? (fn [{:keys [type contract]}]
                                                                   (and (= type :tick) (= contract con)))
                                                         reduce-fn (fn [accum {:keys [field value]}]
                                                                     (assoc accum field value))
                                                         done? (fn [accum]
                                                                 (let [received-fields (-> accum keys set)]
                                                                   (every? received-fields fields)))
                                                         complete (partial g/cancel-market-data con)]
                                                     (reduce-responses accept? reduce-fn done? complete
                                                                       g/request-market-data con)))

  (defn execute-order
    "Executes an order, returning only when the order is filled.

Ex.:
user> (execute-order {:type :equity :symbol \"IBKR\" :exchange \"SMART\"}
                     {:type :limit :action :buy :quantity 100 :limit-price 14.68})
({:status :filled, :last-fill-price 14.68, :average-fill-price 14.68, :order-id 21, :client-id 100, :filled 100, :type :order-status, :parent-id 0, :remaining 0, :why-held nil, :permanent-id 377488187} {:type :open-order, :order-id 21, :contract {:put-call-right :unknown, :include-expired? false, :type :equity, :currency \"USD\", :local-symbol \"IBKR\", :exchange \"SMART\", :symbol \"IBKR\", :contract-id 43645865}, :order {:time-in-force :day, :order-id 21, :client-id 100, :discretionary-amount 0.0, :action :buy, :quantity 100, :sweep-to-fill? false, :limit-price 14.68, :outside-regular-trading-hours? false, :transmit? true, :stop-price 0.0, :hidden? false, :type :limit, :all-or-none? false, :block-order? false, :permanent-id 377488187}, :order-state {:maximum-commission 1.7976931348623157E308, :minimum-commission 1.7976931348623157E308, :commission 1.7976931348623157E308, :equity-with-loan \"1.7976931348623157E308\", :maintenance-margin \"1.7976931348623157E308\", :initial-margin \"1.7976931348623157E308\", :status :filled}})"
  [contract order]
  (let [order-id (g/get-order-id)
        accept? #(= order-id (:order-id %))
        reduce-fn (fn [coll x] (log/debug x) (conj coll x))
        done? (partial some (fn [{:keys [type status]}]
                              (and (= :order-status type) (= :filled status))))]
    (log/debug "Order-Id: " order-id)
    (reduce-responses accept? reduce-fn done? nil g/place-order
                      order-id contract order)))

(defn get-historical-data [contract end-time duration duration-unit bar-size
                           bar-size-unit what-to-show
                           use-regular-trading-hours?]
  "Gets historical price bars for a contract.

Example:
user> (get-historical-data {:type :equity :symbol \"IBKR\" :exchange \"SMART\"}
                                                (clj-time.core/date-time 2012 10 18)
                                                5 :days 15 :minutes :trades true)
({:has-gaps? false, :volume 724, :trade-count 456, :close 14.76, :low 14.66, :high 14.8, :open 14.66, :time #<DateTime 2012-10-17T19:45:00.000Z>} {:has-gaps? false, :volume 346, :trade-count 222, :close 14.66, :low 14.6, :high 14.7, :open 14.64, :time #<DateTime 2012-10-17T19:30:00.000Z>}..."
  (let [request-id (g/get-request-id)
        accept? #(= request-id (:request-id %))
        reduce-fn conj
        done? (partial some #(= :price-bar-complete (:type %)))
        complete (fn [])
        responses (reduce-responses accept? reduce-fn done? complete
                                    g/request-historical-data request-id
                                    contract end-time duration duration-unit
                                    bar-size bar-size-unit what-to-show
                                    use-regular-trading-hours?)
        errors (filter #(= :error (:type %)) responses)]
    (if (not (empty? errors))
      errors
      (->> responses
           (filter #(= :price-bar (:type %)))
           (map #(select-keys % [:time :open :high :low :close
                                 :trade-count :volume :has-gaps?]))))))

(defn get-open-orders
  "user> (get-open-orders)
({:order-state {:maximum-commission 1.7976931348623157E308, :minimum-commission 1.7976931348623157E308, :commission 1.7976931348623157E308, :equity-with-loan \"1.7976931348623157E308\", :maintenance-margin \"1.7976931348623157E308\", :initial-margin \"1.7976931348623157E308\", :status :submitted}, :order {:time-in-force :day, :order-id 22, :client-id 100, :discretionary-amount 0.0, :action :buy, :quantity 100, :sweep-to-fill? false, :limit-price 14.62, :outside-regular-trading-hours? false, :transmit? true, :stop-price 0.0, :hidden? false, :type :limit, :all-or-none? false, :block-order? false, :permanent-id 377488188}, :contract {:put-call-right :unknown, :include-expired? false, :type :equity, :currency \"USD\", :local-symbol \"IBKR\", :exchange \"SMART\", :symbol \"IBKR\", :contract-id 43645865}, :order-id 22} ...)"
  []
  (let [accept? #(#{:open-order :open-order-end :error} (:type %))
        reduce-fn conj
        done? (partial some #(= :open-order-end (:type %)))
        complete (fn [])
        responses (reduce-responses accept? reduce-fn done? complete
                                    g/request-open-orders)
        errors (filter #(= :error (:type %)) responses)]
    (if (empty? errors)
      (->> responses
           (filter #(= :open-order (:type %)))
           (map #(select-keys % [:order-id :contract :order
                                 :order-state])))
      errors)))
