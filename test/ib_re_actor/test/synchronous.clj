(ns ib-re-actor.test.synchronous
  (require [ib-re-actor.gateway :refer [subscribe is-end-for?]]
           [ib-re-actor.synchronous :refer :all]
           [midje.sweet :refer :all]
           [midje.util :refer [testable-privates]]))

(testable-privates ib-re-actor.synchronous/reduce-responses)

(defn always-true [_] true)
(defn always-false [_] false)
(defn get-responses
  "since reduce-responses is asynchronous, we need to wrap them
in a future we can wait on with a timeout or it's possible to hang
here forever."
  []
  (-> (future (#'ib-re-actor.synchronous/reduce-responses
               always-true conj always-false nil (fn [])))
      (deref 100 nil)))

(def not-connected-msg {:type :error, :request-id -1, :code 504,
                        :message "Not connected"})

(def general-error-msg {:type :error :code 1})

(defmacro with-messages [messages & body]
  `(with-redefs [subscribe (fn [h#] (doseq [m# ~messages] (h# m#)))]
     (let [~'responses (get-responses)]
       ~@body)))

(fact "getting all responses for synchronous commands"
      (fact "when not connected"
            (with-messages [not-connected-msg]
              (fact "it stops" responses => (contains not-connected-msg))))
      (fact "when there is a general error"
            (with-messages [general-error-msg]
              (fact "it stops" responses => (contains general-error-msg))))
      (fact "when there are some good messages followed by an error"
            (with-messages [..message1.. ..message2.. general-error-msg]
              (fact "it stops" responses => (contains general-error-msg))
              (fact "it still gives me the other messages"
                    responses => (contains [..message1.. ..message2..]))))
      (fact "when there are good messages followed by an end message"
            (with-messages [..message1.. ..message2.. ..end-message..]
              (fact "it gives me all the messages"
                    responses => (contains [..message1.. ..message2.. ..end-message..])
                    (provided (is-end-for? 1 ..end-message..) => true)))))
