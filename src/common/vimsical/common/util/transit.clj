(ns vimsical.common.util.transit
  (:require
   [clojure.core.async :as a]
   [cognitect.transit :as transit]
   [clojure.core.async.impl.protocols :as ap])
  (:import
   (clojure.lang PersistentTreeMap)
   (com.cognitect.transit ReadHandler WriteHandler)
   (java.io ByteArrayOutputStream)
   (java.nio.channels ReadableByteChannel)))

(declare read-transit write-transit write-transit*)

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
  (some->> req
           (content-type)
           (re-find transit-content-type-re)
           (second)
           (keyword)))

(defn decode-transit-request
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

(declare
 chan?
 chan->transit-readable-byte-channel)

(defn- transit-response?
  [{:keys [body] :as response}]
  (or (transit-request? response) (coll? body)))

(defn encode-transit-response
  [{:keys [body] :as response}
   writer
   {:as   options
    :keys [content-type-key]
    :or   {content-type-key "content-type"}}]
  (cond
    (chan? body)
    (update response :body chan->transit-readable-byte-channel)

    (transit-response? response)
    (-> response
        (assoc-in [:headers content-type-key] "application/transit+json")
        (update :body write-transit writer options))

    :else response))

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

(defn read-transit-stream
  ([in]
   (read-transit-stream in default-reader default-reader-options))
  ([in options]
   (read-transit-stream in default-reader (merge default-reader-options options)))
  ([in reader options]
   (letfn [(str->bytes [^String s] (.getBytes s))]
     (let [options' (merge default-reader-options options)
           in'      (cond-> in
                      (string? in) (str->bytes)
                      true         (clojure.java.io/input-stream))
           r        (reader in' options')]
       (read-transit-stream in' reader r options'))))
  ([in reader r options]
   (when-some [x (try (transit/read r) (catch Throwable _))]
     (lazy-seq
      (cons x (read-transit-stream in reader r options))))))

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
  [out options]
  (transit/writer out :json (merge default-writer-options options)))

(defn write-transit
  ([object] (write-transit object default-writer default-writer-options))
  ([object options] (write-transit object default-writer (merge default-writer-options options)))
  ([object writer options]
   (let [baos (ByteArrayOutputStream.)
         transit-writer    (writer baos options)
         _    (transit/write transit-writer object)
         ret  (.toString baos)]
     (.reset baos)
     ret)))

;;
;; * Core.async chans
;;

(defn- chan? [chan]
  (and (not (coll? chan))
       (not (string? chan))
       (satisfies? ap/ReadPort chan)))

(let [open-bracket-bytes  (.getBytes "[")
      close-bracket-bytes (.getBytes "]")
      separator-bytes     (.getBytes ",")]
  (defn chan->transit-readable-byte-channel
    "Return a `ReadableByteChannel` that will contain the transit-encoded
  contents of chan. The transit payload is produced by wrapping the contents of
  the chan inside vector characters, and interposing commas between elements.

  This is intended as a way to:

  - encode channels as vectors without realizing them in memory
  - work around a poor implementation for core.async response bodies in pedestal

  c.f https://github.com/pedestal/pedestal/blob/master/service/src/io/pedestal/http/impl/servlet_interceptor.clj#L105"
    ^ReadableByteChannel
    [chan & [{:keys [writer writer-options]
              :or   {writer         default-writer
                     writer-options {}}}]]
    ;; The piped os is passed to the transit writer, it will pass the written
    ;; data without copying it to the piped is
    (let [input-stream       (java.io.PipedInputStream.)
          readable-byte-chan (java.nio.channels.Channels/newChannel input-stream)
          output-stream      (java.io.PipedOutputStream. input-stream)
          transit-writer     (writer output-stream writer-options)]
      ;; The rule of thumb here is to avoid i/o in the go-loop, if all goes
      ;; according to plan we'll get full backpressure starting from the
      ;; ReadableByteChannel all the way to the cassandra result set chan. This
      ;; means that if we have a slow client writing to our byte channel _will_
      ;; block so we defer every write operation to a thread while we park.
      (do
        ;; Start the write loop
        (a/go
          (try
            (loop [started? false]
              (if-some [x (a/<! chan)]
                (do (a/<!
                     (a/thread
                       ;; Precede the first value by "[" and remaining values by ","
                       (if started?
                         (.write output-stream separator-bytes)
                         (.write output-stream open-bracket-bytes))
                       (transit/write transit-writer x)))
                    (recur true))
                ;; Make sure to only close the vector if we started, or write an
                ;; empty vector if the channel closed
                (a/<!
                 (a/thread
                   (when-not started?
                     (.write output-stream open-bracket-bytes)  )
                   (.write output-stream close-bracket-bytes)))))
            (finally
              (.close output-stream))))
        ;; Make sure we return the byte-chan
        readable-byte-chan))))

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

