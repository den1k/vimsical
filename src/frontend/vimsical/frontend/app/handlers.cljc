(ns vimsical.frontend.app.handlers
  (:refer-clojure :exclude [uuid])
  (:require
   [day8.re-frame.async-flow-fx]
   [re-frame.core :as re-frame]
   [vimsical.frontend.db :as db]
   [re-frame.interceptor :as interceptor]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.router.handlers :as router.handlers]
   [vimsical.frontend.router.routes :as router.routes]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.frontend.vcs.sync.handlers :as vcs.sync.handlers]
   [vimsical.frontend.vims.db :as vims.db]
   [vimsical.frontend.vims.handlers :as vims.handlers]
   [vimsical.vims :as vims]
   [vimsical.frontend.util.re-frame :as util.re-frame]))

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

(re-frame/reg-event-fx
 ::set-vims
 (fn [{:keys [db]} [_ vims {:keys [route?] :or {route? true}}]]
   (let [vims-ref (util.mg/->ref db vims)
         vims-ent (util.mg/->entity db vims)]
     (cond-> {:db (assoc db :app/vims vims-ref)}
       route? (assoc :dispatch-n [[::router.handlers/route ::router.routes/vims vims-ent]])))))

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
       [::vims.handlers/load-vims-async-did-complete vims-uid options]
       [::vcs.sync.handlers/start vims-uid]
       [::set-vims vims-uid]]})))

;;
;; * Open
;;

(defmethod router.handlers/did-unmount-route-fx-handler ::router.routes/vims
  [{:keys [db]} [_ {::router.routes/keys [args]}]]
  ;; If the current vims is still the same one that was unmounted we need to
  ;; clear the state.
  (let [route-vims-ref (util.mg/->ref db args)
        app-vims-ref   (util.mg/->ref db :app/vims)
        dissoc-vims?   (= route-vims-ref app-vims-ref)]
    (cond-> {:dispatch [::close-vims args]}
      dissoc-vims? (assoc :db (dissoc db :app/vims) ))))

;; NOTE only do this when the route is mounted by the history since we need the
;; side-effects for loading the vims
(defmethod router.handlers/did-mount-history-route-fx-handler ::router.routes/vims
  [{:keys [db]} [_ {::router.routes/keys [args]}]]
  {:dispatch [::open-vims args]})

(re-frame/reg-event-fx
 ::open-vims
 (fn [{:keys [db]} [_ vims {:keys [uuid-fn] :or {uuid-fn uuid} :as options}]]
   (let [[_ vims-uid] (util.mg/->ref db vims)]
     {:dispatch-n [[::vims.handlers/load-vims vims options]
                   [::vcs.sync.handlers/start vims-uid]
                   [::set-vims vims]
                   [::close-modal]]})))

(defmethod router.handlers/did-mount-history-route-fx-handler ::router.routes/embed
  [{:keys [db]} [_ {::router.routes/keys [args] :as route}]]
  {:dispatch [::open-embed args]})

(re-frame/reg-event-fx
 ::open-embed
 (fn [{:keys [db]} [_ vims {:keys [uuid-fn] :or {uuid-fn uuid} :as options}]]
   (let [[_ vims-uid] (util.mg/->ref db vims)]
     {:dispatch-n
      [[::set-vims vims {:route? false}]
       [::vims.handlers/load-vims vims-uid options]]})))

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
