(ns vimsical.backend.handlers.auth.commands
  (:require
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.server.interceptors.session :as session]
   [vimsical.backend.handlers.multi :refer [handle]]
   [vimsical.backend.util.auth :as util.auth]
   [vimsical.remotes.backend.auth.commands :as commands]
   [vimsical.user :as user]))

;;
;; * Queries
;;

(def authenticate-user-query
  '[:find ?uid ?password
    :in $ ?email
    :where
    [?e :db/uid ?uid]
    [?e ::user/email ?email]
    [?e ::user/password ?password]])

;;
;; * Handlers
;;

(defmethod handle ::commands/login!
  [{:keys [datomic] :as context} [_ login-user]]
  (letfn [(authenticate-user [{:keys [conn]} {::user/keys [email password]}]
            (let [[[uid password-hash]] (vec (datomic/q datomic authenticate-user-query email))]
              (and (util.auth/check-password password password-hash) uid)))]
    (if-let [uid (authenticate-user datomic login-user)]
      (session/set-session context (session/recreate {::user/uid uid}))
      (session/set-session context session/empty-session))))

(defmethod handle ::commands/register!
  [{:keys [datomic] :as context} [_ {:keys [db/uid] :as register-user}]]
  (letfn [(hash-user-password [user]
            (update user ::user/password util.auth/hash-password))
          (transact-user! [user]
            (let [tx (hash-user-password user)]
              (deref (datomic/transact datomic [tx]))))]
    (try
      (transact-user! register-user)
      (-> context
          (assoc-in [:response :body] [::commands/register-success])
          (session/set-session context (session/recreate {::user/uid uid})))
      (catch Throwable t
        (session/set-session context session/empty-session)))))
