(ns vimsical.frontend.router.handlers
  (:require
   [bidi.bidi :as bidi]
   [re-frame.core :as re-frame]
   [vimsical.frontend.router.interop :as interop]
   [vimsical.frontend.router.routes :as routes]))

;;
;; * History Fx
;;

(defonce history
  (interop/new-history
   (fn [{:keys [handler route-params] :as v}]
     (re-frame/dispatch [::history-route handler route-params]))
   routes/routes))

(defn- args-seq
  [args]
  (->> args
       ;; Remove entries where values are nil
       (reduce-kv
        (fn [m k v]
          (if (some? v)
            (assoc m k v)
            m))
        (empty args))
       ;; Turn into seq
       (seq)
       ;; Flatten
       (apply concat)))

(defn- history-fx
  [{::routes/keys [route-handler args]}]
  (let [path (apply bidi/path-for routes/routes route-handler (args-seq args))]
    (interop/replace-token history path)))

(re-frame/reg-fx :history (comp history-fx routes/encode-route))

;;
;; * Route lifecycle
;;

(defn route-fx-dispatch
  [coeffects [_ {::routes/keys [route-handler] :as route}]] route-handler)

(defmulti did-mount-route-fx-handler route-fx-dispatch)
(defmethod did-mount-route-fx-handler :default [_ _])
(re-frame/reg-event-fx ::did-mount-route did-mount-route-fx-handler)

(defmulti did-unmount-route-fx-handler route-fx-dispatch)
(defmethod did-unmount-route-fx-handler :default [_ _])
(re-frame/reg-event-fx ::did-unmount-route did-unmount-route-fx-handler)

(defmulti did-mount-history-route-fx-handler route-fx-dispatch)
(defmethod did-mount-history-route-fx-handler :default [_ _])
(re-frame/reg-event-fx ::did-mount-history-route did-mount-history-route-fx-handler)

(defmulti did-unmount-history-route-fx-handler route-fx-dispatch)
(defmethod did-unmount-history-route-fx-handler :default [_ _])
(re-frame/reg-event-fx ::did-unmount-history-route did-unmount-history-route-fx-handler)

;;
;; * Handlers
;;

(re-frame/reg-event-fx
 ::init
 (fn [_ _] (interop/init history)))

(re-frame/reg-event-fx
 ::route
 (fn [{:keys [db]} [_ route-handler route-params]]
   (let [app-route (:app/route db)
         new-route (routes/decode-route (routes/new-route route-handler route-params))]
     (when-not (routes/route= app-route new-route)
       {:history new-route
        :db      (assoc db :app/route new-route)
        :dispatch-n
        [[::did-unmount-route app-route]
         [::did-mount-route new-route]]}))))

(re-frame/reg-event-fx
 ::history-route
 (fn [{:keys [db]} [_ route-handler route-params]]
   (let [app-route (:app/route db)
         new-route (routes/decode-route (routes/new-route route-handler route-params))]
     (when-not (routes/route= app-route new-route)
       {:db (assoc db :app/route new-route)
        :dispatch-n
        [[::did-unmount-history-route app-route]
         [::did-mount-history-route new-route]]}))))
