(ns ib-re-actor.util)

(def translations
  {:boolean-int {:out 'ib-re-actor.conversions/translate-from-ib-boolean-int
                 :in 'ib-re-actor.conversions/translate-to-ib-boolean-int}
   :double-string {:out (fn [x] (Double/parseDouble x))
                   :in 'clojure.core/str}
   :expiry {:out 'ib-re-actor.conversions/translate-from-ib-expiry
            :in 'ib-re-actor.conversions/translate-to-ib-expiry}
   :order-action {:out 'ib-re-actor.conversions/translate-from-ib-order-action
                  :in 'ib-re-actor.conversions/translate-to-ib-order-action}
   :order-type {:out 'ib-re-actor.conversions/translate-from-ib-order-type
                :in 'ib-re-actor.conversions/translate-to-ib-order-type}
   :security-type {:out 'ib-re-actor.conversions/translate-from-ib-security-type
                   :in 'ib-re-actor.conversions/translate-to-ib-security-type}})

(defn get-options [{:keys [translate-in translate-out translation]}]
  {:translate-in
   (if translate-in translate-in
       (get-in translations [translation :in] identity))
   :translate-out
   (if translate-out translate-out
       (get-in translations [translation :out] identity))})

(defmacro field-props [& property-descriptors]
  (reduce (fn [coll [name field & kw-args]]
            (let [{:keys [translate-in translate-out]} (get-options kw-args)]
              (assoc coll (keyword name)
                     `(fn
                        ([this#]
                           (~translate-out (. this# ~field)))
                        ([this# val#]
                           (set! (. this# ~field)
                                 (~translate-in val#)))))))
          {} property-descriptors))

(defmacro field-props-immutable [& property-descriptors]
  (reduce (fn [coll [name field & kw-args]]
            (let [{:keys [translate-in translate-out]} (get-options kw-args)]
              (assoc coll (keyword name)
                     `(fn
                        ([this#]
                           (~translate-in (. this# ~field)))
                        ([this# val#]
                           (let [clone# (.clone this#)]
                             (set! (. clone# ~field)
                                   (~translate-out val#))
                             clone#))))))
          {} property-descriptors))

(defmacro if-set
  ([attributes key object field]
     `(if-set ~attributes ~key ~object ~field ~identity))
  ([attributes key object field xform]
     `(if-let [x# (get ~attributes ~key)]
        (set! (. ~object ~field) (~xform x#)))))

(defn if-assoc
  ([attributes key val]
     (if val
       (assoc attributes key val)
       attributes))
  ([attributes key val xform]
     (if val
       (assoc attributes key (xform val))
       attributes)))

(defn- map-of-fields
  "Like bean, but uses fields instead of properties"
  [obj]
  (let [c (.getClass obj)
        fmap (reduce #(assoc %1 (.getName %2) (.get %2 obj)) {} (.getDeclaredFields c))
        v (fn [k] ((pmap k)))
        snapshot (fn []
                   (reduce (fn [m e]
                             (assoc m (key e) ((val e))))
                           {} (seq fmap)))]
    (proxy [clojure.lang.APersistentMap]
        []
      (containsKey [k]
        (contains? fmap k))
      (entryAt [k]
        (when (contains? fmap k)
          (new clojure.lang.MapEntry k (v k))))
      (valAt
        ([k]
           (v k))
        ([k default]
           (if (contains? pmap k)
             (v k)
             default)))
      (cons [m]
        (conj (snapshot) m))
      (count []
        (count pmap))
      (assoc [k v]
        (assoc (snapshot) k v))
      (without [k]
        (dissoc (snapshot) k))
      (seq [] ((fn thisfn [plseq]
                 (lazy-seq
                  (when-let [pseq (seq plseq)]
                    (cons (new clojure.lang.MapEntry (first pseq) (v (first pseq)))
                          (thisfn (rest pseq)))))) (keys pmap))))))
