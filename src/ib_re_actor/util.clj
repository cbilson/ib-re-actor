(ns ib-re-actor.util)

(defmacro field-based-property
  ([property-name field-name]
     `(defn ~property-name
        ([x#] (. x# ~field-name))
        ([x# val#] (set! (. x# ~field-name) val#) x#)))
  ([property-name field-name get-xform set-xform]
     `(defn ~property-name
        ([x#] (~get-xform (. x# ~field-name)))
        ([x# val#] (set! (. x# ~field-name) (~set-xform val#)) x#))))

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