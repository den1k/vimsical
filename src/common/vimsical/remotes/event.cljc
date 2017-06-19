(ns vimsical.remotes.event
  (:require [clojure.spec.alpha :as s]))

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
    (seq x)      (first x)
    :else        (throw (ex-info "Invalid event, expected keyword or vector" {:event x}))))

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

(defmulti error-spec dispatch)
(s/def ::error (s/multi-spec result-spec dispatch))
