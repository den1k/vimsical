(ns vimsical.common.uuid
  #?@(:clj
      [
       (:require [clojure.spec.alpha :as s])
       (:import java.util.UUID)]
      :cljs
      [(:refer-clojure :exclude [uuid])
       (:require
        [cljs-uuid-utils.core :as uuid]
        [clojure.spec.alpha :as s])]))

(s/def :db/uid uuid?)

#?(:cljs
   (defn make-random-squuid
     "(make-random-squuid)  =>  new-uuid
  Arguments and Values:
  new-squuid --- new type 4 (pseudo randomly generated) cljs.core/UUID instance.
  Description:
  Returns pseudo randomly generated, semi-sequential SQUUID.
  See http://docs.datomic.com/clojure/#datomic.api/squuid
  Returns a UUID where the most significant 32 bits are the current time since epoch in seconds.
  like: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx as per http://www.ietf.org/rfc/rfc4122.txt.
  Examples:
  (make-random-squuid)  =>  #uuid \"305e764d-b451-47ae-a90d-5db782ac1f2e\"
  (type (make-random-squuid)) => cljs.core/UUID"
     []
     (letfn [(top-32-bits [] (.toString (int (/ (.getTime (js/Date.)) 1000)) 16))
             (f [] (.toString (rand-int 16) 16))
             (g [] (.toString (bit-or 0x8 (bit-and 0x3 (rand-int 15))) 16))]
       (UUID.
        (str
         (top-32-bits) "-"
         (f) (f) (f) (f) "-4"
         (f) (f) (f) "-"
         (g)
         (f) (f) (f) "-"
         (f) (f) (f) (f) (f) (f) (f) (f) (f) (f) (f) (f))
        nil))))

(defn uuid
  ([] #?(:cljs (make-random-squuid)
         :clj
         ;; TODO check this!
         (let [uuid      (java.util.UUID/randomUUID)
               time      (System/currentTimeMillis)
               secs      (quot time 1000)
               lsb       (.getLeastSignificantBits uuid)
               msb       (.getMostSignificantBits uuid)
               timed-msb (bit-or
                          (bit-shift-left secs 32)
                          (bit-and 0x00000000ffffffff msb))]
           (java.util.UUID. timed-msb lsb))))
  ([str]
   #?(:cljs (clojure.core/uuid str)
      :clj  (java.util.UUID/fromString ^String str))))
