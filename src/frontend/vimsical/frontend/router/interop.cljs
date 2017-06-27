(ns vimsical.frontend.router.interop
  (:require
   [bidi.bidi :as bidi]
   [clojure.spec.alpha :as s]
   [pushy.core :as pushy])
  (:import (goog.Uri)))

;;
;; * Routes and query-params
;;

(defn match-route [routes path]
  (let [match        (bidi/match-route routes path)
        uri          (goog.Uri. path)
        query-data   (.getQueryData uri)
        query-keys   (js->clj (.getKeys query-data))
        query-vals   (js->clj (.getValues query-data))
        query-params (zipmap (map keyword query-keys) query-vals)]
    (update match :route-params merge query-params)))

;;
;; * History
;;

(s/fdef new-history
        :args (s/cat :f ifn? :routes vector?))

(defn new-history
  [f routes]
  (pushy/pushy f (partial match-route routes)))

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
