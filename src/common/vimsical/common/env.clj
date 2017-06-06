(ns vimsical.common.env
  (:refer-clojure :exclude [reset!])
  (:require
   [clojure.java.io :as io]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [environ.core :as env]))

;;
;; * Mutable env map
;;

(defonce env-atom (atom env/env))

(defn reset! [key value] (do (swap! env-atom assoc key value) nil))
(defn get!   [key] (get @env-atom key))

;;
;; * Env (:test, :dev, :prod)
;;

(s/fdef target :ret #{:test :dev :prod})

(defn env [] (-> env-atom deref :env keyword))

(defmacro with-env
  "Same as a let binding for environment values, meant to be used in tests."
  [m & body]
  `(let [before# (deref env-atom)]
     (try
       (clojure.core/swap! env-atom merge ~m)
       (do ~@body)
       (finally
         (clojure.core/reset! env-atom before#)))))

(comment
  (do
    (assert (nil? (get! ::foobar)))
    (with-env {::foobar :bar!}
      (assert (= :bar! (get! ::foobar))))
    (assert (nil? (get! ::foobar)))))

;;
;; * Conformer helpers
;;

(defn- valid! [key spec-or-conformer x]
  (if (not= ::s/invalid x)
    x
    (throw
     (ex-info
      "Invalid env value"
      {:key  key
       :env  (get! key)
       :spec spec-or-conformer}))))

(defn- not-blank [s] (when-not (str/blank? s) s))

(defn conformer
  [conform-fn]
  (s/conformer
   (fn [string]
     (some-> string not-blank conform-fn))))

;;
;; * Conformer specs
;;

(s/def ::boolean (conformer (fn [^String s] (Boolean/valueOf s))))
(s/def ::int     (conformer (fn [^String s] (Integer/parseInt s))))
(s/def ::long    (conformer (fn [^String s] (Long/parseLong s))))
(s/def ::double  (conformer (fn [^String s] (Double/parseDouble s))))
(s/def ::ratio   (conformer read-string))
(s/def ::file    (conformer io/file))
(s/def ::string  identity)
(s/def ::keyword (conformer keyword))

;;
;; * High-order conformers
;;

(defn ns-keyword [ns]
  (conformer
   (fn [^String s]
     (clojure.core/keyword ns s))))

(defn comma-separated
  [conform-fn]
  (conformer
   (fn [^String s]
     (try
       (let [values  (str/split s #",")
             values' (mapv (partial s/conform conform-fn) values)]
         (if (not-any? #{::s/invalid} values')
           values'
           ::s/invalid))
       (catch Throwable t
         ::s/invalid)))))

;;
;; * API
;;

(defn optional
  ([key] (optional key nil))
  ([key spec-or-conformer]
   (when-some [val (get! key)]
     (cond->> val
       (some? spec-or-conformer) (s/conform spec-or-conformer)
       true                      (valid! key spec-or-conformer)))))

(defn required
  ([key] (required key nil))
  ([key spec-or-conformer]
   (if-some [value (optional key spec-or-conformer)]
     value
     (valid! key spec-or-conformer ::s/invalid))))

(comment
  (reset! ::foo "1,2")
  (required ::foo (comma-separated ::int))
  (reset! ::foo "1,:a")
  (required ::foo (comma-separated ::int)))
