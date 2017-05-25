(ns vimsical.backend.components.session-store
  (:require
   [vimsical.backend.util.log :as log]
   [clojure.spec :as s]
   [ring.middleware.session.store :as store]
   [taoensso.carmine :as car]
   [vimsical.backend.adapters.redis :as redis]
   [vimsical.backend.components.session-store.spec :as spec])
  (:import
   (java.util UUID)
   (vimsical.backend.adapters.redis Redis)))

;;
;; * Internal specs
;;

(s/def ::read-key (s/nilable string?))
(s/def ::write-key (s/nilable ::read-key))

;;
;; * Helpers
;;

(defn- random-key
  []
  (str (UUID/randomUUID)))

;;
;; * Internal API
;;

(s/fdef read-session*
        :args (s/cat :redis ::redis/redis :k ::read-key)
        :ret ::spec/session)

(defn read-session*
  [redis k]
  (try
    (let [session (car/wcar redis (car/get k))]
      (log/debug "read-session..." k session)
      session)
    (catch Throwable t
      (println "Error getting session" {:key k :ex t})
      (throw t))))

(s/fdef write-session*
        :args (s/cat :redis ::redis/redis :k ::write-key :val ::spec/session)
        :ret  ::read-key)

(defn write-session*
  [redis k val]
  (try
    (log/debug "write-session..." k val)
    (let [k' (or k (random-key))]
      (do
        (car/wcar
         redis
         (car/set k' val))
        k'))
    (catch Throwable t
      (println "Error writing session" {:key k :val val :ex t})
      (throw t))))

(s/fdef delete-session*
        :args (s/cat :redis ::redis/redis :k ::read-key))

(defn delete-session*
  [redis k]
  (try
    (do
      (log/debug "delete-session..." k)
      (car/wcar
       redis
       (car/del k))
      nil)
    (catch Throwable t
      (println "Error deleting session" {:key k :ex t})
      (throw t))))

;;
;; * Ring session store
;;

(extend-protocol store/SessionStore
  Redis
  (read-session   [redis k]     (read-session* redis k))
  (write-session  [redis k val] (write-session* redis k val))
  (delete-session [redis k]     (delete-session* redis k)))

;;
;; * Constructor
;;

(defn ->session-store
  [{::keys [host port]}]
  (redis/->redis {:host host :port port}))

(s/def ::session-store (fn [x] (and x (instance? Redis x))))