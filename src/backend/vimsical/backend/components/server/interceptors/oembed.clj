(ns vimsical.backend.components.server.interceptors.oembed
  (:require
   [cheshire.core :as json]
   [clojure.java.io :as io]
   [io.pedestal.interceptor :as interceptor]
   [ring.middleware.params :as p]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.vims :as vims]
   [clojure.string :as str]))

(def embed-width 884)
(def embed-height 500)

;;
;; * Request
;;

(defn vims-url->vims-uid [url]
  (try
    (let [uri            (java.net.URI. url)
          path           (.getPath uri)
          [_ _ vims-uid] (str/split path #"/")]
      (java.util.UUID/fromString vims-uid))
    (catch Throwable _)))

(defn vims-url->vims-data
  [datomic url]
  (let [uid (vims-url->vims-uid url)
        ref [:db/uid uid]
        tx  '[:db/uid (default ::vims/title "Untitled")]]
    (datomic/pull datomic tx ref)))

(defn vims-uid->embed-url
  [uid]
  (str "https://vimsical.com/embed/?vims_uid=" uid))

;;
;; * Response
;;

(defn embed-html
  [vims-uid]
  (let [src     (vims-uid->embed-url vims-uid)
        style   "border:none"
        sandbox "allow-forms allow-pointer-lock allow-popups allow-same-origin allow-scripts"]
    (format
     "<iframe src=\"%s\" width=\"%s\" height=\"%s\" style=\"%s\" sandbox=\"%s\"></iframe>"
     src embed-width embed-height style sandbox)))

(defn new-response-body
  [vims-url vims-uid vims-title]
  (when vims-uid
    {:version       "1.0",
     :type          "rich",
     :html          (embed-html vims-uid)
     :width         embed-width,
     :height        embed-height,
     :title         vims-title,
     :url           vims-url,
     :provider_name "Vimsical",
     :provider_url  "https://vimsical.com/"}))

(defn- add-response-content-type
  [response format]
  (let [content-type (case format "json" "application/json" "application/json")]
    (assoc-in response [:headers "Content-Type"] content-type)))

(defn- encode-response-body
  [response format]
  (let [f (case format "json" json/encode json/encode)]
    (update response :body f)))

;;
;; * Interceptor
;;

(defn- enter [{:keys [datomic] :as context}]
  (let [{:keys [url]}                        (-> context :request :params)
        {:keys [db/uid] ::vims/keys [title]} (vims-url->vims-data datomic url)
        body                                 (new-response-body url uid title)]
    (if body
      (assoc context :response {:status 200 :body body})
      (assoc context :response {:status 404}))))

(defn- leave [{:keys [datomic] :as context}]
  (let [{:keys [format]} (-> context :request :params)]
    (-> context
        (update :response add-response-content-type format)
        (update :response encode-response-body format))))

(def handle-embed
  (interceptor/interceptor
   {:name  ::oembed
    :enter enter
    :leave leave}))
