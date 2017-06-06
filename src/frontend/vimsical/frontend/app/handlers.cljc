(ns vimsical.frontend.app.handlers
  (:refer-clojure :exclude [uuid])
  (:require
   [day8.re-frame.async-flow-fx]
   [re-frame.core :as re-frame]
   [re-frame.interceptor :as interceptor]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]
   [vimsical.frontend.vcs.sync.handlers :as vcs.sync.handlers]
   [vimsical.frontend.vims.db :as vims.db]
   [vimsical.frontend.vims.handlers :as vims.handlers]
   [vimsical.vims :as vims]))

;;
;; * Routes
;;

(re-frame/reg-event-db
 ::route
 (fn [db [_ route]]
   (assoc db :app/route route)))

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
  [uuid-fn vims-uid]
  {:pre [(ifn? uuid-fn) (uuid? vims-uid)]}
  (letfn [(event+uuid [[id uuid]] [id uuid])]
    {:id                [::open-vims-async vims-uid]
     :first-dispatch    [::open-vims-async-did-start vims-uid]
     :event-id->seen-fn {::vims.handlers/deltas-success event+uuid
                         ::vims.handlers/vims-success   event+uuid}
     :rules             [{:when     :seen-all-of?
                          :events   [[::vims.handlers/deltas-success vims-uid] [::vims.handlers/vims-success vims-uid]]
                          :dispatch [::open-vims-async-did-complete uuid-fn vims-uid]}]
     :halt?             true}))

(re-frame/reg-event-fx
 ::open-vims-async-did-start
 (fn [{:keys [db]} [_ vims-uid]]
   {:dispatch-n
    [[::vims.handlers/vims vims-uid]
     [::vims.handlers/deltas vims-uid]]}))

(re-frame/reg-event-fx
 ::open-vims-async-did-complete
 (fn [{:keys [db]} [_ uuid-fn vims-uid deltas]]
   (let [deltas (or deltas (vims.db/get-deltas db vims-uid))]
     {:dispatch-n
      [[::vcs.handlers/init uuid-fn vims-uid deltas]
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
   (let [vims-ref (util.mg/->ref db vims)]
     {:db         (assoc db :app/vims vims-ref)
      :dispatch-n [[::route :route/vims]]})))

;;
;; * New
;;

;; Reuse the did-complete handler but don't start the async flow since all the
;; data is local
(re-frame/reg-event-fx
 ::new-vims
 (fn [{:keys [db]} [_ uuid-fn owner]]
   {:pre [uuid-fn owner]}
   (let [vims-uid (uuid-fn)]
     {:dispatch-n
      [[::vims.handlers/new owner nil {:uid vims-uid}]
       [::set-vims vims-uid]
       [::open-vims-async-did-complete uuid-fn vims-uid]]})))

;;
;; * Open
;;

(re-frame/reg-event-fx
 ::open-vims
 (fn [{:keys [db]} [_ uuid-fn {::vims/keys [vcs] :as vims}]]
   {:pre [vims]}
   (let [[_ vims-uid :as vims-ref] (util.mg/->ref db vims)
         async-load?               (nil? vcs)]
     (cond-> {:dispatch-n [[::set-vims vims]]}
       async-load? (assoc :async-flow (open-vims-async-flow uuid-fn vims-uid))))))

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
