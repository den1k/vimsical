(ns vimsical.backend.components.server.interceptors.index
  (:require
   [clojure.java.io :as io]
   [io.pedestal.interceptor :as interceptor]
   [ring.util.response :as response]))

(defn- not-found?
  [context]
  (some-> context :response :status (== 404)))

(def index
  (let [file     (io/file (io/resource "public/index.html"))
        path     (str file)
        response (-> (response/file-response path)
                     (response/content-type "text/html"))]
    (interceptor/interceptor
     {:id ::index
      :leave
      (fn [context]
        (if (not-found? context)
          (assoc context :response response)
          context))})))
