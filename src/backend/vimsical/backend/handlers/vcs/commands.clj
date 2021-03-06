(ns vimsical.backend.handlers.vcs.commands
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.delta-store :as delta-store]
   [vimsical.backend.components.delta-store.protocol :as delta-store.protocol]
   [vimsical.backend.components.server.interceptors.event-auth :as event-auth]
   [vimsical.backend.components.session-store :as session-store]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.util.async :refer [<?]]
   [vimsical.remotes.backend.vcs.commands :as commands]
   [vimsical.user :as user]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.lib :as lib]
   [vimsical.vims :as vims]))

;;
;; * Context specs
;;

(s/def ::datomic-context (s/keys :req-un [::datomic/datomic]))
(s/def ::deltas-context (s/keys :req-un [::delta-store/delta-store]))

;;
;; * Branch
;;

(defmethod multi/handle-event ::commands/add-branch
  [{:keys [datomic] :as context} [_ vims-uid branch]]
  (let [tx  {:db/uid vims-uid ::vims/branches branch}]
    (multi/async
     context
     (multi/set-response context (<? (datomic/transact-chan datomic tx))))))

;;
;; * Deltas
;;

(defmethod event-auth/require-auth? ::commands/add-deltas [_] true)
(defmethod multi/handle-event ::commands/add-deltas
  [{:as         context
    :keys       [delta-store session]
    ::user/keys [uid]}
   [_ vims-uid deltas :as event]]
  (let [vims-session-path (session-store/vims-session-path vims-uid)
        vims-session      (get-in session vims-session-path)
        vims-session'     (delta-store/vims-session vims-session deltas)
        insert-chan       (delta-store.protocol/insert-deltas-chan delta-store vims-uid uid vims-session deltas)]
    (multi/async
     context
     (-> context
         (multi/set-response (<? insert-chan))
         (multi/assoc-in-session vims-session-path vims-session')))))

;;
;; * Libs
;;

(defmethod event-auth/require-auth? ::commands/add-lib [_] true)
(defmethod multi/handle-event ::commands/add-lib
  [{:as context :keys [datomic]} [_ branch-uid lib :as event]]
  (let [tx {:db/uid branch-uid ::branch/libs [lib]}]
    (multi/async
     context
     (multi/set-response context (<? (datomic/transact-chan datomic tx))))))

(defmethod event-auth/require-auth? ::commands/remove-lib [_] true)
(defmethod multi/handle-event ::commands/remove-lib
  [{:as context :keys [datomic]} [_ branch-uid {::lib/keys [src] :as lib} :as event]]
  (let [tx [[:db/retract
             [:db/uid branch-uid]
             ::branch/libs
             [::lib/src src]]]]
    (multi/async
     context
     (multi/set-response context (<? (datomic/transact-chan datomic tx))))))
