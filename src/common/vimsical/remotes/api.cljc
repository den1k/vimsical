(ns vimsical.remotes.api
  (:require
   [clojure.spec :as s]
   [vimsical.remotes.error :as error]
   [vimsical.remotes.event :as event]
   [vimsical.remotes.api.auth.commands :as auth.commands]))

;;
;; * Generic event
;;

(defmulti event-args first)
(s/def ::event-args (s/multi-spec event-args first))

(s/def ::event
  (s/& ::event/event
       (fn [{:keys [id args] :as a}]
         (s/conform ::event-args [id args]))))

;;
;; * Generic response
;;

(defmulti query-response-args first)
(s/def ::query-reponse-args (s/multi-spec query-response-args first))
(s/def ::query-response
  (s/& ::event/event
       (fn [{:keys [id args] :as a}]
         (s/conform ::query-response-args [id args]))))
(s/def ::command-response empty?)
(s/def ::response
  (s/or :error ::error/error :command ::command-response :query ::query-response))

;;
;; * Event args
;;

;;
;; ** Auth
;;

(defmethod event-args ::auth.commands/login!    [_] ::auth.commands/login-args)
(defmethod event-args ::auth.commands/logout!   [_] ::auth.commands/logout-args)
(defmethod event-args ::auth.commands/register! [_] ::auth.commands/register-args)
