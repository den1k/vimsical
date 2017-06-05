(ns vimsical.frontend.vcs.sync.db
  (:require
   [clojure.spec :as s]
   [re-frame.interceptor :as interceptor]
   [re-frame.std-interceptors :as std-interceptors]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]
   [vimsical.vims :as vims]))

(declare path)

;;
;; * Fsm
;;

;; NOTE ignoring ns due to circular dependency
(def fsm
  `{Wait  {:start              Init
           :start-new          Ready}
    Init  {:init-success       Ready
           :init-error         InitError}
    Ready {:sync               Ready
           :sync-success       Ready
           :sync-error         SyncError
           :add-deltas         Ready
           :add-branch         Ready
           :add-deltas-success Ready
           :add-deltas-error   Sync
           :add-branch-success Ready
           :add-branch-error   BranchError}})

;;
;; * Spec
;;

(s/def ::state (set (filter symbol? (tree-seq map? #(into (keys %) (vals %)) fsm))))
(s/def ::deltas (s/every ::delta/delta))
(s/def ::branch (s/every ::branch/branch))
(s/def ::delta (s/keys :req-un [::delta/uid ::delta/prev-uid ::delta/branch-uid]))
(s/def ::delta-by-branch-uid (s/every-kv ::branch/uid ::delta))
(s/def ::sync (s/keys :req [::state ::deltas ::branch ::delta-by-branch-uid]))

;;
;; * FSM interceptor
;;

(def kw-name (comp keyword name))

(def fsm-check-allowed-transition
  (interceptor/->interceptor
   :id     ::fsm
   :before
   (fn fsm-check-allowed-transition-before
     [context]
     (let [db                            (interceptor/get-coeffect context :db)
           [event-id vims-uid :as event] (interceptor/get-coeffect context :event)
           {::keys [state]}              (get-in db (path vims-uid))
           allowed-transitions           (get fsm state)]
       (if-not (and allowed-transitions (contains? allowed-transitions (kw-name event-id)))
         (throw (ex-info "Transition" {:current state :event event :allowed allowed-transitions}))
         context)))))

(def fsm-enrich-next-state-transition
  (std-interceptors/enrich
   (fn fsm-enrich-next-state-transition-after
     [db [event-id vims-uid :as event]]
     (if-some [{::keys [state]} (get-in db (path vims-uid))]
       (if-some [next-state (get-in fsm [state (kw-name event-id)])]
         (assoc-in db (path vims-uid ::state) next-state)
         (throw (ex-info "State transition not found" {:state state :event event})))
       db))))

(def fsm-interceptor
  [fsm-check-allowed-transition
   fsm-enrich-next-state-transition])

;;
;; * Constructors
;;

(defn new-sync [vims-uid]
  {::state               `Init
   ::deltas              []
   ::branch              []
   ::delta-by-branch-uid {}})

(s/def ::state-key #{::state ::deltas ::branch ::delta-by-branch-uid})

(s/fdef path :args (s/cat :vims-uid ::vims/uid :?ks (s/? (s/* ::state-key))) )
(defn   path [vims-uid & ks] (into [:app/sync vims-uid] ks))
