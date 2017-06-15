(ns vimsical.backend.handlers.auth.queries
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
   [vimsical.remotes.backend.auth.queries :as queries]
   [vimsical.user :as user]))

;;
;; * Spec
;;

(s/def ::context-deps
  (s/keys :req-un [::datomic/datomic ::session-store/session-store]))

;;
;; * Datomic queries
;;

(def invite-query
  '[:find (pull ?e [:db/uid ::user/first-name ::user/last-name]) .
    :in $ ?token
    :where [?e ::user/invite-token ?token]])

(defn invite-chan
  [datomic token]
  (datomic/q-chan datomic invite-query token))

;;
;; * Invite
;;

(defmethod multi/context-spec ::queries/invite [_] ::context-deps)
(defmethod multi/handle-event ::queries/invite
  [{:keys [datomic] :as context} [_ token]]
  (multi/async
   context
   (multi/set-response
    context
    (async/<? (invite-chan datomic token)))))
