(ns vimsical.backend.handlers.auth.commands
  (:require
   [vimsical.backend.util.log :as log]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.server.interceptors.session :as session]
   [vimsical.backend.components.session-store :as session-store]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.util.auth :as util.auth]
   [vimsical.remotes.backend.auth.commands :as commands]
   [vimsical.queries.user :as queries.user]
   [vimsical.user :as user]
   [clojure.spec :as s]))

;;
;; * Spec
;;

(s/def ::context-deps
  (s/keys :req-un [::datomic/datomic ::session-store/session-store]))

;;
;; * Session helpers
;;

(defn create-user-session [context user-uid]
  (assoc context :session ^:recreate {::user/uid user-uid}))

(defn clear-session [context]
  (dissoc context :session))

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

(defn pull-user
  [{:keys [datomic]} user-uid]
  (datomic/pull datomic queries.user/datomic-pull-query [:db/uid user-uid]))

;;
;; * Login
;;

(defmethod multi/context-spec ::commands/login [_] ::context-deps)
(defmethod multi/handle-event ::commands/login
  [{:keys [datomic] :as context} [_ login-user]]
  (letfn [(authenticate-user [{:keys [conn]} {::user/keys [email password]}]
            (let [[[uid password-hash]] (vec (datomic/q datomic authenticate-user-query email))]
              (and (util.auth/check-password password password-hash) uid)))]
    (if-let [uid (authenticate-user datomic login-user)]
      (let [user (pull-user context uid)]
        (-> context
            (create-user-session uid)
            (assoc-in [:response :body] user)))
      (clear-session context))))

(defmethod multi/handle-event ::commands/logout
  [context _]
  (clear-session context))

;;
;; * Registration
;;

(defmethod multi/context-spec ::commands/register [_] ::context-deps)
(defmethod multi/handle-event ::commands/register
  [{:keys [datomic] :as context} [_ {:keys [db/uid] :as register-user}]]
  (letfn [(hash-user-password [user]
            (update user ::user/password util.auth/hash-password))
          (transact-user! [user]
            (let [tx (hash-user-password user)]
              (deref (datomic/transact datomic [tx]))))]
    (let [_    (transact-user! register-user)
          user (pull-user context uid)]
      (-> context
          (create-user-session uid)
          (assoc-in [:response :body] user)))))
