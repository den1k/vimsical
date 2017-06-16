(ns vimsical.backend.handlers.auth.commands
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.session-store :as session-store]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.handlers.user.queries :as user.queries]
   [vimsical.backend.util.async :as async]
   [vimsical.backend.util.auth :as util.auth]
   [vimsical.common.uuid :refer [uuid]]
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

(defmethod multi/context-spec ::commands/signup [_] ::context-deps)
(defmethod multi/handle-event ::commands/signup
  [{:keys [datomic] :as context} [_ {:keys [db/uid] :as signup-user :or {uid (uuid)}}]]
  (assert uid)
  (letfn [(hash-user-password [user]
            (update user ::user/password util.auth/hash-password))
          (user-tx-chan [user]
            (async/thread-try
             (deref (datomic/transact datomic (hash-user-password user)))))]
    (multi/async
     context
     (let [_    (async/<? (user-tx-chan signup-user))
           user (async/<? (user.queries/user+snapshots-chan context uid))]
       (-> context
           (create-user-session uid)
           (multi/set-response user))))))

;;
;; * Login
;;

(defmethod multi/context-spec ::commands/login [_] ::context-deps)
(defmethod multi/handle-event ::commands/login
  [{:keys [datomic] :as context} [_ login-user]]
  (letfn [(user-uid-chan [{:keys [conn]} {::user/keys [email password]}]
            (async/thread-try
             (when-let [[[uid password-hash]] (vec (datomic/q datomic authenticate-user-query email))]
               (and (util.auth/check-password password password-hash) uid))))]
    (multi/async
     context
     (if-let [uid  (async/<? (user-uid-chan datomic login-user))]
       (let [user (async/<? (user.queries/user+snapshots-chan context uid))]
         (-> context
             (create-user-session uid)
             (multi/set-response user)))
       (multi/set-response context 401 {:reason "Invalid email or password"})))))

(defmethod multi/handle-event ::commands/logout
  [context _]
  (multi/delete-session context))

