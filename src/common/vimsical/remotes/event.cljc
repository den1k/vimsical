(ns vimsical.remotes.event
  (:require [clojure.spec :as s]))

;;
;; * Internal event spec
;;

(s/def ::id keyword?)
(s/def ::args (s/* any?))
(s/def ::event-vec (s/cat :id ::id :args ::args))

;;
;; * Event helpers
;;

(defn id [x]
  (cond
    (keyword? x) x
    (vector? x)  (first x)
    :else        (assert false)))

(defn id-append
  "Given qualified `kw`, append `segment` to its namespace."
  [kw segment]
  (let [key-namespace (namespace kw)
        segment-name  (name segment)
        key-name      (name kw)
        append-name   (str key-name "-" segment-name)]
    (keyword key-namespace append-name)))

;;
;; * Extensible event spec (dispatch on id)
;;

(def dispatch id)

;;
;; ** Event
;;

(defmulti event-spec dispatch)
(s/def ::event (s/multi-spec event-spec dispatch))

;;
;; ** Result
;;

(defmulti result-spec dispatch)
(s/def ::result (s/multi-spec result-spec dispatch))
(s/def ::command-success empty?)

;;
;; ** Error
;;

;; NOTE could have a multi-spec here too...

(s/def ::msg string?)
(s/def ::data any?)
(s/def ::cause string?)
(s/def ::error (s/keys :opt [::msg ::data ::cause]))

(defn throwable->error [t]
  (-> t Throwable->map (select-keys [:msg])))
