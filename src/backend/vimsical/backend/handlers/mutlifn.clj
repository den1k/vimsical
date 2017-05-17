(ns vimsical.backend.handlers.mutlifn
  (:require
   [clojure.spec :as s]
   [vimsical.remotes.backend :as backend]))

;;
;; * Handler dispatch
;;

(defn handler-dispatch [context [id]] id)

;;
;; * Context spec
;;

(defmulti context-mspec handler-dispatch)
(s/def ::context (s/multi-spec context-mspec first))

;;
;; * Event spec
;;

(s/def ::event (fn [context event] (s/conform ::backend/event event)))

;;
;; * Args spec
;;

(s/def ::args (s/and ::context ::event))

;;
;; * Multimethod
;;

(defmulti handle handler-dispatch)
