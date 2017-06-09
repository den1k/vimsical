(ns vimsical.frontend.router.interop
  (:require
   [bidi.bidi :as bidi]
   [clojure.spec.alpha :as s]
   [pushy.core :as pushy]))

;;
;; * History
;;

(s/fdef new-history
        :args (s/cat :f ifn? :routes vector?))

(defn new-history
  [f routes]
  (pushy/pushy f (partial bidi/match-route routes)))

;; NOTE We need to wait until the app is initialized before we init the router,
;; or we'll get a dispatch before we got a chance to setup our db etc. However
;; this "swallows" the first route dispatch, so we fake it here by setting the
;; token to its current value
(defn init [history]
  (do (pushy/start! history)
      (pushy/set-token! history (pushy/get-token history))))

(defn replace-token
  [history path]
  (pushy/replace-token! history path))
