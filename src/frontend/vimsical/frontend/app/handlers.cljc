(ns vimsical.frontend.app.handlers
  (:refer-clojure :exclude [uuid])
  (:require
   [day8.re-frame.async-flow-fx]
   [re-frame.core :as re-frame]
   [re-frame.interceptor :as interceptor]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.router.handlers :as router.handlers]
   [vimsical.frontend.router.routes :as router.routes]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]
   [vimsical.frontend.vcs.sync.handlers :as vcs.sync.handlers]
   [vimsical.frontend.vims.db :as vims.db]
   [vimsical.frontend.vims.handlers :as vims.handlers]
   [vimsical.vims :as vims]))

;;
;; * Modal
;;

(re-frame/reg-event-db
 ::modal
 (fn [db [_ modal]]
   (assoc db :app/modal modal)))

(re-frame/reg-event-db
 ::close-modal
 (fn [db _] (assoc db :app/modal nil)))

(re-frame/reg-event-db
 ::toggle-modal
 (fn [{:as db cur-modal :app/modal} [_ modal]]
   (let [next-modal (when (not= cur-modal modal) modal)]
     (assoc db :app/modal next-modal))))

;;
;; * Async loading flow
;;

(defn open-vims-async-flow
  [vims-uid {:keys [uuid-fn] :or {uuid-fn uuid} :as options}]
  {:pre [(uuid? vims-uid)]}
  (letfn [(event+uuid [[id uuid]] [id uuid])]
    {:id                [::open-vims-async vims-uid]
     :first-dispatch    [::open-vims-async-did-start vims-uid]
     :event-id->seen-fn {::vims.handlers/deltas-success event+uuid
                         ::vims.handlers/vims-success   event+uuid}
     :rules             [{:when     :seen-all-of?
                          :events   [[::vims.handlers/deltas-success vims-uid]
                                     [::vims.handlers/vims-success vims-uid]]
                          :dispatch [::open-vims-async-did-complete vims-uid options]}]
     :halt?             true}))

(re-frame/reg-event-fx
 ::open-vims-async-did-start
 (fn [{:keys [db]} [_ vims-uid]]
   {:dispatch-n
    [[::vims.handlers/vims vims-uid]
     [::vims.handlers/deltas vims-uid]]}))

(re-frame/reg-event-fx
 ::open-vims-async-did-complete
 (fn [{:keys [db]} [_ vims-uid deltas {:keys [uuid-fn] :or {uuid-fn uuid} :as options}]]
   (let [deltas (or deltas (vims.db/get-deltas db vims-uid))]
     {:dispatch-n
      [[::vcs.handlers/init vims-uid deltas options]
       [::vcs.sync.handlers/start vims-uid]]})))

;;
;; * Current vims
;;

(def close-prev-vims-interceptor
  "Interceptor that dispatches ::close-vims with the previous :app/vims."
  (interceptor/->interceptor
   :id    ::close-vims-interceptor
   :after
   (fn [context]
     (let [{before :app/vims} (interceptor/get-coeffect context :db)
           {after :app/vims}  (interceptor/get-effect context :db)
           changed?           (some-> before (not= after))]
       (cond-> context
         changed? (update-in [:effects :dispatch-n] conj [::close-vims before]))))))

(re-frame/reg-event-fx
 ::set-vims
 [close-prev-vims-interceptor]
 (fn [{:keys [db]} [_ vims]]
   (let [vims-ref (util.mg/->ref db vims)
         vims-ent (util.mg/->entity db vims)]
     {:db         (assoc db :app/vims vims-ref)
      :dispatch-n [[::router.handlers/route ::router.routes/vims vims-ent]]})))

;;
;; * New
;;

;; Reuse the did-complete handler but don't start the async flow since all the
;; data is local
(re-frame/reg-event-fx
 ::new-vims
 (fn [{:keys [db]} [_ owner {:keys [uuid-fn] :or {uuid-fn uuid} :as options}]]
   {:pre [owner]}
   (let [vims-uid (uuid-fn)]
     {:dispatch-n
      [[::vims.handlers/new owner nil {:uid vims-uid}]
       [::set-vims vims-uid]
       [::open-vims-async-did-complete vims-uid options]]})))

;;
;; * Open
;;

(defmethod router.handlers/route-dispatch ::router.routes/vims
  [{::router.routes/keys [args]} {:keys [db]}]
  (when args
    {:dispatch [::open-vims args]}))

(re-frame/reg-event-fx
 ::open-vims
 (fn [{:keys [db]} [_ {::vims/keys [vcs] :as vims} {:keys [uuid-fn] :or {uuid-fn uuid} :as options}]]
   {:pre [vims]}
   (when vims
     (let [[_ vims-uid] (util.mg/->ref db vims)
           async-load?  (nil? vcs)]
       (cond-> {:dispatch-n
                [[::set-vims vims]
                 [::close-modal]]}
         async-load? (assoc :async-flow (open-vims-async-flow vims-uid options)))))))

;;
;; * Close
;;

(re-frame/reg-event-fx
 ::close-vims
 (fn [{:keys [db]} [_ vims]]
   {:pre [vims]}
   (let [[_ vims-uid] (util.mg/->ref db vims)]
     {:dispatch-n
      [[::vims.handlers/update-snapshots {:db/uid vims-uid}]
       [::vcs.sync.handlers/stop vims-uid]]})))
