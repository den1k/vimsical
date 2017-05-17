(ns vimsical.backend.handlers.auth.commands
  (:require
   [datomic.api :as d]
   [vimsical.backend.components.session-store :as session-store]
   [vimsical.backend.handlers.mutlifn :refer [handle]]
   [vimsical.backend.util.auth :as util.auth]
   [vimsical.remotes.backend.auth.commands :as commands]
   [vimsical.user :as user]))

;;
;; * Queries
;;

(def authenticate-user-query
  '[:find ?uuid ?password
    :in $ ?email
    :where
    [?e :db/uuid ?uuid]
    [?e :user/email ?email]
    [?e :user/password ?password]])

;;
;; * Handlers
;;

(defmethod handle ::commands/login!
  [{:keys [::datomic] :as context} [_ login-user]]
  (letfn [(authenticate-user
            [{:keys [conn]} {::user/keys [email password]}]
            (let [[[db-id password-hash]] (vec (d/q authenticate-user-query (d/db conn) email))]
              (and (util.auth/check-password password password-hash) db-id)))]
    (if-let [db-id (authenticate-user datomic login-user)]
      {:session {::user/id db-id}}
      {:session session-store/empty-session})))
