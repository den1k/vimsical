(ns vimsical.frontend.figwheel-handler
  (:require
   [bidi.bidi :as bidi]
   [ring.middleware.resource :as resource]
   [ring.util.response :as response]
   [clojure.java.io :as io]))

;;
;; * Pages
;;

(defn- resource-path [path] (str (io/file (io/resource path))))

(defn- index [_] (response/file-response (resource-path "public/index.html")))
(defn- embed [_] (response/file-response (resource-path "public/player.html")))

;;
;; * Routes
;;

(def ^:private routes
  ["/"
   {["embed/" :db/uid] embed
    true               index}])

;;
;; * Handler
;;

(defn- pages
  [{:keys [uri] :as req}]
  (try
    (let [{:keys [handler]} (bidi/match-route routes uri)]
      (handler req))
    (catch Throwable t req)))

(def handler
  (resource/wrap-resource pages "/public"))
