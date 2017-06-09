(ns vimsical.frontend.figwheel-handler
  (:require
   [ring.middleware.resource :as resource]
   [ring.util.response :as response]
   [clojure.java.io :as io]))

(def index-path (str (io/file (io/resource "public/index.html"))))

(defn index
  [req]
  (response/file-response index-path ))

(def handler
  (-> index
      (resource/wrap-resource "/public")))
