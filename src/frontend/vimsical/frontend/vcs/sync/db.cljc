(ns vimsical.frontend.vcs.sync.db
  (:require
   [clojure.spec :as s]
   [re-frame.interceptor :as interceptor]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]))

(declare path)

;;
;; * Fsm
;;

;; NOTE ignoring ns due to circular dependency
(def fsm
  `{Wait   {:start                        Init}
    Init   {:delta-by-branch-uid-success  Sync
            :delta-by-branch-uid-error    InitError}
    Sync   {:sync                         Sync ; ??
            :sync-success                 Ready
            :sync-error                   SyncError}
    Ready  {:add-deltas                   Deltas
            :add-branch                   Branch}
    Deltas {:add-deltas-success           Ready
            :add-deltas-error             Sync}
    Branch {:add-branch-success           Ready
            :add-branch-error             BranchError}})

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

(def kw-name (comp keyword name))

(defn fsm-check-allowed-transition
  [{:keys [coeffects] :as context}]
  (let [{[event-id vims-uid :as event] :event :keys [db]} coeffects
        {::keys [state]}                                  (get-in db (path vims-uid))
        allowed-transitions                               (get fsm state)]
    (if-not (contains? allowed-transitions (kw-name event-id))
      (throw (ex-info "Transition" {:allowed allowed-transitions :event event}))
      context)))

(defn fsm-next-state-transition
  [{:keys [coeffects] :as context}]
  (let [{[event-id vims-uid :as event] :event :keys [db]} coeffects
        {::keys [state]}                                  (get-in db (path vims-uid))
        next-state                                        (get-in fsm [state (kw-name event-id)])
        db'                                               (assoc-in db (path vims-uid ::state) next-state)]
    (assoc-in context [:effects :db] db')))

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
