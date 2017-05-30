(ns vimsical.frontend.vcs.sync.db
  (:require
   [clojure.spec :as s]
   [re-frame.interceptor :as interceptor]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]))

;;
;; * Fsm
;;

(def fsm
  `{Wait   {::start                        Init}
    Init   {::deltas-by-branch-uid-success Sync
            ::deltas-by-branch-uid-error   InitError}
    Sync   {::sync-success                 Ready
            ::sync-error                   SyncError}
    Ready  {::add-deltas                   Deltas
            ::add-branch                   Branch}
    Deltas {::add-deltas-success           Ready
            ::add-deltas-error             Sync}
    Branch {::add-branch-success           Ready
            ::add-branch-error             BranchError}})

;;
;; * Spec
;;

(s/def ::state (set (filter symbol? (tree-seq map? #(into (keys %) (vals %)) fsm))))
(s/def ::deltas (s/every ::delta/delta))
(s/def ::branch (s/every ::branch/branch))
(s/def ::delta (s/keys :req-un [::delta/uid ::delta/prev-uid ::delta/branch-uid]))
(s/def ::deltas-by-branch-uid (s/every-kv ::branch/uid ::delta))

(s/def ::sync (s/keys :req [::state ::deltas ::branch ::deltas-by-branch-uid]))

;;
;; * FSM interceptor
;;

(defn fsm-check-allowed-transition
  [{[event-id vims-uid :as event] :event :keys [db] :as context}]
  (let [{::keys [state]} (get-in db [:app/sync vims-uid])
        allowed-transitions (get fsm state)]
    (if-not (contains? allowed-transitions event-id)
      (throw (ex-info "Transition" {:allowed allowed-transitions :event event}))
      context)))

(defn fsm-next-state-transition
  [{[event-id vims-uid] :event :keys [db]}]
  (let [{::keys [state]} (get-in db [:app/sync vims-uid])
        next-state          (get-in fsm [state event-id])]
    {:db (assoc-in [:app/sync vims-uid ::state] next-state)}))

(def fsm-interceptor
  (interceptor/->interceptor
   :id     ::fsm
   :before fsm-check-allowed-transition
   :after  fsm-next-state-transition))

;;
;; * Constructor
;;

(defn new-sync [vims-uid]
  {::state                `Init
   ::deltas               []
   ::branch               []
   ::deltas-by-branch-uid {}})

(defn path [vims-uid & ks]
  (into [:app/sync vims-uid] ks))
