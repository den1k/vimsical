(ns vimsical.common.util.transit
  (:require
   [io.pedestal.interceptor :as interceptor]
   [cognitect.transit :as transit])
  (:import
   (java.io ByteArrayOutputStream)
   (com.cognitect.transit TransitFactory WriteHandler ReadHandler)
   (clojure.lang PersistentTreeMap)))

(declare read-transit write-transit)

;;
;; * HTTP
;;

;;
;; ** Content Types
;;

(def ^:private transit-content-type-re #"^application/transit\+(json|msgpack)")
(defn- get-header [req header] (some-> req :headers (get header)))
(defn- content-type [req]
  (or (get-header req "content-type")
      (get-header req "Content-Type")))

;;
;; ** Requests
;;

(defn- transit-request?
  [req]
  (some->> (content-type req)
           (re-find transit-content-type-re)
           (second)
           (keyword)))

(defn- decode-transit-request
  [req reader options]
  (if-not (transit-request? req)
    req
    (update req :body
            (fn [body]
              (-> body
                  (reader options)
                  (transit/read))))))

;;
;; ** Responses
;;

(defn- transit-response?
  [{:keys [body] :as response}]
  (or (transit-request? response) (coll? body)))

(defn- encode-transit-response
  [resp writer {:keys [content-type-key] :as options :or {content-type-key "content-type"}}]
  (if-not (transit-response? resp)
    resp
    (-> resp
        (assoc-in [:headers content-type-key] "application/transit+json")
        (update :body write-transit writer options))))

;;
;; * Reader
;;

(def default-reader-options
  {:handlers
   {"sorted-map"
    (reify
      ReadHandler
      (fromRep [_ x] (into (sorted-map) x)))}})

(defn default-reader
  [in options]
  (transit/reader in :json (merge default-reader-options options)))

(defn read-transit
  ([in-or-string]
   (read-transit in-or-string default-reader default-reader-options))
  ([in-or-string options]
   (read-transit in-or-string default-reader (merge default-reader-options options)))
  ([in-or-string reader options]
   (letfn [(str->bytes [^String s] (.getBytes s))
           (transit-read [in reader options]
             (-> in
                 (clojure.java.io/input-stream)
                 (reader options)
                 (transit/read)))]
     (cond-> in-or-string
       (string? in-or-string) (str->bytes)
       true                   (transit-read reader options)))))

;;
;; * Writer
;;

(def default-writer-options
  {:handlers
   {PersistentTreeMap
    (reify
      WriteHandler
      (tag [_ _] "sorted-map")
      (rep [_ x] (into {} x)))}})

(defn default-writer
  [in options]
  (transit/writer in :json (merge default-writer-options options)))

(defn write-transit
  ([object] (write-transit object default-writer default-writer-options))
  ([object options] (write-transit object default-writer (merge default-writer-options options)))
  ([object writer options]
   (let [baos (ByteArrayOutputStream.)
         w    (writer baos options)
         _    (transit/write w object)
         ret  (.toString baos)]
     (.reset baos)
     ret)))

;;
;; * Ring middleware
;;

(defn wrap-transit-body
  ([handler] (wrap-transit-body handler nil))
  ([handler {:keys [reader reader-options writer writer-options]
             :or   {reader default-reader reader-options {}
                    writer default-writer writer-options {}}}]
   (fn [req]
     (-> req
         (decode-transit-request reader reader-options)
         (handler)
         (encode-transit-response writer writer-options)))))

;;
;; * Pedestal interceptor
;;

(defn new-transit-body-interceptor
  ([] (new-transit-body-interceptor nil))
  ([{:keys [reader reader-options writer writer-options]
     :or   {reader default-reader reader-options {}
            writer default-writer writer-options {}}}]
   ;; Difference in case between Ring and Pedestal??
   (let [writer-options' (assoc writer-options :content-type-key "Content-Type")]
     (interceptor/interceptor
      {:name  ::transit-body-interceptor
       :enter (fn [context]
                (update context :request decode-transit-request reader reader-options))
       :leave (fn [context]
                (update context :response encode-transit-response writer writer-options'))}))))
