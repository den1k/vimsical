(ns vimsical.common.util.transit-test
  #?@(:clj
      [(:require
        [clojure.core.async :as a]
        [clojure.test :refer [deftest is]]
        [vimsical.common.util.transit :as sut])]
      :cljs
      [(:require
        [clojure.test :refer [deftest is]]
        [vimsical.common.util.transit :as sut])]))

(deftest io-test
  (let [val    {:foo {:bar (into (sorted-map) {2 2 1 1})}}
        string "[\"^ \",\"~:foo\",[\"^ \",\"~:bar\",[\"~#sorted-map\",[\"^ \",\"~i1\",1,\"~i2\",2]]]]"]
    (is (= val (sut/read-transit string)))
    (is (= string (sut/write-transit val)))
    (is (= val (-> val sut/write-transit sut/read-transit)))))

#?(:clj
   ;; For
   (defn transit-readable-byte-channel->string
     [^java.nio.channels.ReadableByteChannel rch]
     (slurp (java.nio.channels.Channels/newInputStream rch))))

#?(:clj
   (deftest chan-encoding-test
     (let [coll        [{:a 1} {:b 2}]
           chan        (a/to-chan coll)
           encoded-rch (sut/chan->transit-readable-byte-channel chan)
           encoded-str (transit-readable-byte-channel->string encoded-rch)
           decoded     (sut/read-transit encoded-str)]
       (is (= coll decoded)))))
