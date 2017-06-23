(ns vimsical.vcs.data.indexed.vector-impl
  (:refer-clojure :exclude [vec vector vector?])
  (:require
   [net.cgrand.xforms :as x]
   [clojure.spec.alpha :as s]))

;;
;; * Index
;;

(defn index
  ([] (clojure.core/vector))
  ([vals] (clojure.core/vec vals))
  ([f vals] (x/into (index) (map f) vals)))

;;
;; * Vector
;;

(s/def ::vector clojure.core/vector?)
(def vec clojure.core/vec)
(def vector clojure.core/vector)
