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
  [redis session-key]
  (try
    (let [session (car/wcar redis (car/get session-key))]
      (log/debug "read-session..." session-key session)
      session)
    (catch Throwable t
      (log/error "Error getting session" {:key session-key :ex t})
      (throw t))))

(s/fdef write-session*
        :args (s/cat :redis ::redis/redis :k ::write-key :val ::spec/session)
        :ret  ::read-key)

(defn write-session*
  [redis session-key value]
  (try
    (log/debug "write-session..." session-key value)
    (let [session-key' (or session-key (random-key))]
      (do
        (car/wcar
         redis
         (car/set session-key' value))
        session-key'))
    (catch Throwable t
      (log/error "Error writing session" {:key session-key :val value :ex t})
      (throw t))))

(s/fdef delete-session*
        :args (s/cat :redis ::redis/redis :k ::read-key))

(defn delete-session*
  [redis session-key]
  (try
    (do
      (log/debug "delete-session..." session-key)
      (car/wcar
       redis
       (car/del session-key))
      nil)
    (catch Throwable t
      (log/error "Error deleting session" {:key session-key :ex t})
      (throw t))))

;;
;; * Ring session store
;;

(extend-protocol store/SessionStore
  Redis
  (read-session   [redis session-key]       (read-session* redis session-key))
  (write-session  [redis session-key value] (write-session* redis session-key value))
  (delete-session [redis session-key]       (delete-session* redis session-key)))

;;
;; * Constructor
;;

(defn ->session-store
  [{::keys [host port]}]
  (redis/->redis {:host host :port port}))

(s/def ::session-store (fn [x] (and x (instance? Redis x))))
