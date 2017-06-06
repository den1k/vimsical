(ns vimsical.frontend.app.handlers
  (:refer-clojure :exclude [uuid])
  (:require
   [re-frame.core :as re-frame]
   [day8.re-frame.async-flow-fx]
   [vimsical.common.util.core :as util]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.app.subs :as subs]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.frontend.vims.handlers :as vims.handlers]
   [vimsical.frontend.vims.db :as vims.db]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]
   [vimsical.frontend.vcs.sync.handlers :as vcs.sync.handlers]
   [vimsical.vims :as vims]
   [re-frame.interceptor :as interceptor]))

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
;; * Current vims
;;

(def close-prev-vims-interceptor
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
 ::close-vims
 (fn [{:keys [db]} [_ vims]]
   {:pre [vims]}
   (let [[_ vims-uid] (util.mg/->ref db vims)]
     {:dispatch-n
      [[::vims.handlers/update-snapshots {:db/uid vims-uid}]
       [::vcs.sync.handlers/stop vims-uid]]})))

(defn new-open-vims-async-flow
  [uuid-fn vims-uid]
  {:pre [(ifn? uuid-fn) (uuid? vims-uid)]}
  {:id                [::open-vims-async vims-uid]
   :first-dispatch    [::open-vims-async-did-start vims-uid]
   :event-id->seen-fn {::vims.handlers/deltas-success
                       (fn [[_ vims-uid]]
                         [::vims.handlers/deltas-success vims-uid])
                       ::vims.handlers/vims-success
                       (fn [[_ vims-uid]]
                         [::vims.handlers/vims-success vims-uid])}
   :rules             [{:when     :seen-all-of?
                        :events   [[::vims.handlers/deltas-success vims-uid]
                                   [::vims.handlers/vims-success vims-uid]]
                        :dispatch [::open-vims-async-did-complete uuid-fn vims-uid]}]
   :halt?             true})

(re-frame/reg-event-fx
 ::open-vims-async-did-start
 (fn [{:keys [db]} [_ vims-uid]]
   {:dispatch-n
    [[::vims.handlers/vims vims-uid]
     [::vims.handlers/deltas vims-uid]]}))

(re-frame/reg-event-fx
 ::open-vims-async-did-complete
 (fn [{:keys [db]} [_ uuid-fn vims-uid]]
   (let [deltas (vims.db/get-deltas db vims-uid)]
     {:dispatch-n
      [[::vcs.handlers/init uuid-fn vims-uid deltas]
       [::vcs.sync.handlers/start vims-uid]]})))

(re-frame/reg-event-fx
 ::open-vims
 [close-prev-vims-interceptor]
 (fn [{:keys [db]} [_ {::vims/keys [vcs] :as vims}]]
   {:pre [vims]}
   (let [[_ vims-uid :as vims-ref] (util.mg/->ref db vims)
         async-load?               (nil? vcs)]
     (cond-> {:db (assoc db :app/vims vims-ref)
              :dispatch-n [[::route :route/vims]]}
       async-load? (assoc :async-flow (new-open-vims-async-flow uuid vims-uid))))))

(re-frame/reg-event-fx
 ::new-vims
 (fn [{:keys [db]} [_ owner]]
   {:pre [owner]}
   (let [vims-uid (uuid)]
     {:dispatch-n
      [[::vims.handlers/new owner nil {:uid vims-uid}]
       [::vcs.handlers/init uuid vims-uid []]
       [::open-vims-async-did-complete vims-uid]]})))
