(ns re-actor.util)

(defmacro field-based-property
  ([property-name field-name]
     `(defn ~property-name
        ([x#] (. x# ~field-name))
        ([x# val#] (set! (. x# ~field-name) val#) x#)))
  ([property-name field-name get-xform set-xform]
     `(defn ~property-name
        ([x#] (~get-xform (. x# ~field-name)))
        ([x# val#] (set! (. x# ~field-name) (~set-xform val#)) x#))))
