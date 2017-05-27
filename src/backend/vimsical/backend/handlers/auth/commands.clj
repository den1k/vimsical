(ns vimsical.backend.handlers.auth.commands
  (:require
   [clojure.spec :as s]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.session-store :as session-store]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.util.async :as async]
   [vimsical.backend.util.auth :as util.auth]
   [vimsical.queries.user :as queries.user]
   [vimsical.remotes.backend.auth.commands :as commands]
   [vimsical.user :as user]))

;;
;; * Spec
;;

(s/def ::context-deps
  (s/keys :req-un [::datomic/datomic ::session-store/session-store]))

;;
;; * Session helpers
;;

(defn create-user-session
  [context user-uid]
  (multi/reset-session context {::user/uid user-uid}))

;;
;; * Datomic queries
;;

(def authenticate-user-query
  '[:find ?uid ?password
    :in $ ?email
    :where
    [?e :db/uid ?uid]
    [?e ::user/email ?email]
    [?e ::user/password ?password]])

(defn user-chan
  [{:keys [datomic]} user-uid]
  (datomic/pull-chan datomic queries.user/datomic-pull-query [:db/uid user-uid]))

;;
;; * Registration
;;

(defmethod multi/context-spec ::commands/register [_] ::context-deps)
(defmethod multi/handle-event ::commands/register
  [{:keys [datomic] :as context} [_ {:keys [db/uid] :as register-user}]]
  (letfn [(hash-user-password [user]
            (update user ::user/password util.auth/hash-password))
          (user-tx-chan [user]
            (async/thread-try
             (let [tx (hash-user-password user)]
               (deref (datomic/transact datomic [tx])))))]
    (multi/async
     context
     (let [_    (async/<? (user-tx-chan register-user))
           user (async/<? (user-chan context uid))]
       (-> context
           (create-user-session uid)
           (multi/set-response user))))))

;;
;; * Login
;;

(defmethod multi/context-spec ::commands/login [_] ::context-deps)
(defmethod multi/handle-event ::commands/login
  [{:keys [datomic] :as context} [_ login-user]]
  (letfn [(authenticate-user [{:keys [conn]} {::user/keys [email password]}]
            (let [[[uid password-hash]] (vec (datomic/q datomic authenticate-user-query email))]
              (and (util.auth/check-password password password-hash) uid)))]
    ;; XXX Go async?
    (if-let [uid (authenticate-user datomic login-user)]
      (multi/async
       context
       (let [user (async/<? (user-chan context uid))]
         (-> context
             (create-user-session uid)
             (multi/set-response user))))
      (multi/delete-session context))))

(defmethod multi/handle-event ::commands/logout
  [context _]
  (multi/delete-session context))

