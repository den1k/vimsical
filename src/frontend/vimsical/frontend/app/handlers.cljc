(ns vimsical.frontend.app.handlers
  (:refer-clojure :exclude [uuid])
  (:require
   [re-frame.core :as re-frame]
   [vimsical.common.util.core :as util]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.app.subs :as subs]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.frontend.vims.handlers :as vims.handlers]
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

(re-frame/reg-event-fx
 ::open-vims
 [close-prev-vims-interceptor]
 (fn [{:keys [db]} [_ {::vims/keys [vcs] :as vims}]]
   {:pre [vims]}
   (let [[_ vims-uid :as vims-ref] (util.mg/->ref db vims)
         load-deltas?              (nil? vcs)]
     {:db (assoc db :app/vims vims-ref)
      :dispatch-n
      ;; XXX status-key to monitor loading?
      (cond-> [[::route :route/vims]]
        load-deltas? (conj [::vims.handlers/deltas uuid vims-uid])
        true         (conj [::vcs.sync.handlers/start vims-uid]))})))

(re-frame/reg-event-fx
 ::new-vims
 (fn [{:keys [db]} [_ owner]]
   {:pre [owner]}
   (let [vims-uid (uuid)]
     {:dispatch-n
      [[::vims.handlers/new owner nil {:uid vims-uid}]
       [::vcs.handlers/init uuid vims-uid []]
       [::open-vims vims-uid]]})))
