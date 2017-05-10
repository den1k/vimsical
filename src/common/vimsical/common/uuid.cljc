(ns vimsical.common.uuid
  #?@(:clj
      [(:require [clojure.spec :as s])
       (:import java.util.UUID)]
      :cljs
      [(:refer-clojure :exclude [uuid])
       (:require
        [cljs-uuid-utils.core :as uuid]
        [clojure.spec :as s])]))

(s/def :db/uuid uuid?)

(defn uuid []
  #?(:cljs (uuid/make-random-squuid)
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