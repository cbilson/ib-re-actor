(ns ib-re-actor.util
  (:use [ib-re-actor.translation]))

(defmacro field-props [& property-descriptors]
  (reduce (fn [implementation [name field & kw-args]]
            (let [{:keys [translation]} (apply hash-map kw-args)
                  this (gensym "this")
                  val (gensym "val")]
              (assoc implementation (keyword name)
                     `(fn
                        ([~this]
                           ~(if translation
                              `(translate :from-ib ~translation (. ~this ~field))
                              `(. ~this ~field)))
                        ([~this ~val]
                           (set! (. ~this ~field)
                                 ~(if translation
                                    `(translate :to-ib ~translation ~val)
                                    val))
                           ~this)))))
          {} property-descriptors))

(defprotocol Mappable
  (to-map [this]
    "Create with a map with the all the non-the non-null properties of object."))

(defn assoc-if [map key val]
  (if val
    (assoc map key val)
    map))