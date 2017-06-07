(ns vimsical.backend.components.session-store
  (:require
   [clojure.spec.alpha :as s]
   [ring.middleware.session.store :as store]
   [taoensso.carmine :as car]
   [vimsical.backend.adapters.redis :as redis]
   [vimsical.backend.util.log :as log]
   [vimsical.user :as user]
   [vimsical.vcs.validation :as vcs.validation]
   [vimsical.vims :as vims])
  (:import
   (java.util UUID)
   (vimsical.backend.adapters.redis Redis)))

;;
;; * Session spec
;;

(s/def ::user-session
  (s/keys :req [::user/uid]))

(s/def ::vims-session
  (s/or :empty empty?
        :active (s/keys :req [::vcs.validation/delta-by-branch-uid ::vcs.validation/order-by-branch-uid])))

(s/def ::vims-session-by-vims-uid
  (s/every-kv ::vims/uid ::vims-session))

(s/def ::vimsae-session
  (s/merge ::user-session (s/keys :req [::vims-session-by-vims-uid])))

(s/def ::session
  (s/or :vimsae ::vimsae-session
        :user ::user-session
        :empty empty?))

(defn vims-session-path
  [vims-uid]
  [::vims-session-by-vims-uid vims-uid])

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
        :ret ::session)

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
        :args (s/cat :redis ::redis/redis :k ::write-key :val ::session)
        :ret  ::read-key)

(defn write-session*
  [redis session-key session]
  (try
    (log/debug "write-session..." session-key session)
    (let [session-key' (or session-key (random-key))]
      (do
        (car/wcar
         redis
         (car/set session-key' session))
        session-key'))
    (catch Throwable t
      (log/error "Error writing session" {:key session-key :val session :ex t})
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
  (read-session   [redis session-key]         (read-session* redis session-key))
  (write-session  [redis session-key session] (write-session* redis session-key session))
  (delete-session [redis session-key]         (delete-session* redis session-key)))

;;
;; * Constructor
;;

(defn ->session-store
  [{::keys [host port]}]
  (redis/->redis {:host host :port port}))

(s/def ::session-store (fn [x] (and x (instance? Redis x))))
