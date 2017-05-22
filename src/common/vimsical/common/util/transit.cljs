(ns vimsical.common.util.transit
  (:require [cognitect.transit :as transit]))

;;
;; * Types
;;

(def sorted-map-write-handler
  (transit/write-handler (constantly "sorted-map") (fn [x] (into {} x))))

(def sorted-map-read-handler
  (transit/read-handler (fn [x] (into (sorted-map) x))))

;;
;; * Writer
;;

(def default-writer-opts
  {:handlers {cljs.core/PersistentTreeMap sorted-map-write-handler}})

(def writer
  (transit/writer :json default-writer-opts))

;;
;; * Reader
;;

(def default-reader-opts
  {:handlers
   {"sorted-map" sorted-map-read-handler
    ;; Transit cljs has its own uuid type?!
    ;; https://groups.google.com/forum/#!topic/clojurescript/_B52tadgUgw
    "u"          uuid}})

(def reader
  (transit/reader :json default-reader-opts))

;;
;; * API
;;

(defn write-transit [data]
  (transit/write writer data))

(defn read-transit [string]
  (transit/read reader string))

(comment
  (let [val {:foo {:bar (into (sorted-map) {2 2 1 1})}}]
    (assert (= val (read-transit (write-transit val))))
    (write-transit val)))
