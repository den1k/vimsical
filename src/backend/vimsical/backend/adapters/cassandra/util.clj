(ns vimsical.backend.adapters.cassandra.util
  (:require [clojure.string :as str]))

;;
;; * Keywords case
;;

;;
;; ** Results
;;
(defn key-fn [s]
  (keyword (str/replace s "_" "-")))

;;
;; ** Params
;;

(defprotocol KeywordCase
  (keyword-replace [_ match replacement]))

(defn keyword-replace-map
  [m match replacement]
  (reduce-kv
   (fn [m k v]
     (assoc m (keyword-replace k match replacement) v))
   (empty m) m))

(extend-protocol KeywordCase
  clojure.lang.Keyword
  (keyword-replace [kw match replacement]
    (-> kw
        (name)
        (str/replace match replacement)
        (keyword)))
  clojure.lang.PersistentVector
  (keyword-replace [v match replacement]
    (mapv #(keyword-replace % match replacement) v))
  clojure.lang.PersistentArrayMap
  (keyword-replace [m match replacement]
    (keyword-replace-map m match replacement))
  clojure.lang.PersistentHashMap
  (keyword-replace [m match replacement]
    (keyword-replace-map m match replacement)))

(defn hyphens->underscores [x] (keyword-replace x "-" "_"))
(defn underscores->hyphens [x] (keyword-replace x "_" "-"))
