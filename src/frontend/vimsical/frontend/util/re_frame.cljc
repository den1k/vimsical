(ns vimsical.frontend.util.re-frame
  #?@(:clj
      [(:require
        [re-frame.core :as re-frame]
        [re-frame.interop :as interop]
        [re-frame.loggers :refer [console]]
        [vimsical.common.uuid :as uuid]
        [vimsical.common.util.core :as util]
        [vimsical.frontend.util.scheduler :as scheduler])]
      :cljs
      [(:require
        [re-frame.core :as re-frame]
        [re-frame.interop :as interop]
        [re-frame.loggers :refer [console]]
        [reagent.ratom :as ratom]
        [vimsical.common.uuid :as uuid]
        [vimsical.common.util.core :as util]
        [vimsical.frontend.util.scheduler :as scheduler])]))

#?(:clj
   (defn- sub-deref
     [sub]
     `(deref (re-frame.core/subscribe ~sub))))

#?(:clj
   (defn- binding-sub-deref
     [[binding sub]]
     `[~binding ~(sub-deref sub)]))

#?(:clj
   (defmacro with-subs
     [bindings & body]
     `(let [~@(apply concat (map binding-sub-deref (partition 2 bindings)))]
        ~@body)))

;;
;; * Subscription helpers
;;

(defn <sub
  [sub]
  (deref (re-frame/subscribe sub)))

;;
;; * Inject sub value in handler
;;

(defn- inject-sub-ignore-dispose-warnings?
  [query-vector]
  (:ignore-warnings (meta query-vector)))

(defn dispose-maybe
  "Dispose of `ratom-or-reaction` if it has no watches."
  [query-vector ratom-or-reaction]
  ;; Behavior notes:
  ;;
  ;; 1. calling `reagent/dispose!` takes care of "walking up" the reaction graph
  ;;    to the nodes that the reaction was `watching` and remove itself from
  ;;    that node's `watches`'.
  ;;
  ;; 2. In turn removing a watch causes that node to dispose of itself iff it
  ;;    has no other watches.
  ;;
  ;; 3. Essentially disposing of a node will dispose of all its dependencies iff
  ;;    they're not needed by another node.
  ;;
  ;; 4. Re-frame adds an on-dispose hook to the reactions returned by
  ;;    `re-frame/subscribe` that will dissoc them from the subscription cache.
  ;;
  ;; There are some potential issues with this scheme:
  ;;
  ;; Imagine a sub that is only used by `inject-sub` and not by components, then
  ;; it will never have watches. This seems like it would be a problem if that
  ;; sub now gets injected in multiple handlers. Due to the disposal behavior
  ;; descibed above, for every handler the cofx would cause:
  ;;
  ;; - subscribe to find the cache empty
  ;; - subscribe to grab the event handler and produce a value
  ;; - subscribe to wrap the value in a reaction
  ;; - the cofx to deref that reaction
  ;; - the cofx to dispose of it (it has no watches)
  ;; - the reaction to remove itself from the cache (on-dispose hook)
  ;;
  ;; We'd basically pay for a cache miss every time we inject that sub?
  ;;
  #?(:cljs
     (when-not (seq (.-watches ratom-or-reaction))
       (when-not (inject-sub-ignore-dispose-warnings? query-vector)
         (console :warn "Disposing of injected sub:" query-vector))
       (interop/dispose! ratom-or-reaction))))

(defmulti inject-sub-cofx
  (fn [context query-vector-or-event->query-vector-fn]
    (if (vector? query-vector-or-event->query-vector-fn)
      ::query-vector
      ::event->query-vector-fn)))

(defmethod inject-sub-cofx ::query-vector
  [context [id :as query-vector]]
  (let [sub (re-frame/subscribe query-vector)
        val (deref sub)]
    (dispose-maybe query-vector sub)
    (assoc context id val)))

(defmethod inject-sub-cofx ::event->query-vector-fn
  [{:keys [event] :as context} event->query-vector-fn]
  (if-some [[id :as query-vector] (event->query-vector-fn event)]
    (let [sub                   (re-frame/subscribe query-vector)
          val                   (deref sub)]
      (dispose-maybe query-vector sub)
      (assoc context id val))
    context))

(defn inject-sub
  "Inject the `:sub` cofx.

  If `query-vector-or-event->query-vector-fn` is a query vector, subscribe and
  dereference that subscription before assoc'ing its value in the context map
  under the id of the subscription and disposing of it.

  If `query-vector-or-event->query-vector-fn` is a fn, it should take a single
  argument which is the event parameters vector for that handler (similar to the
  2-arity of `re-frame.core/reg-sub`). Its return value should be a query-vector
  or nil. From there on the behavior is similar to when passing a query-vector.

  NOTE that if there are no components subscribed to that subscription the cofx
  will dispose of it in order to prevent leaks. However there is a performance
  penalty to doing this since we pay for a re-frame subscription cache miss
  every time we inject it. In such cases the cofx will log a warning which can
  be ignored by setting `:ignore-warnings` on the query vector's meta. A rule of
  thumb for what to do here would be that if an injected sub is disposed of very
  often, we should either rework the subscription graph so that it ends up used
  by a component and thus cached, or we should extract the db lookup logic into
  a function that can be used to get the value straight from the db inside the
  handler. It seems safe to decide to ignore the warning when the disposal
  doesn't happen too often and it is just more convenient to reuse the
  subscription's logic.

  Examples:

  ;; Query vector:

  (re-frame/reg-sub ::injected-static ...)
  (re-frame/reg-event-fx
   ::handler
   [(inject-sub ^:ignore-warnings [::injected-static]]]
   (fn [{:as cofx {::keys [injected-static]} params]
     ...)

  ;; Fn of event to query vector:

  (re-frame/reg-sub ::injected-dynamic (fn [_ [_ arg1 arg2]] ...))
  (re-frame/reg-event-fx
   ::handler
   [(inject-sub
      (fn [[_ event-arg1 event-arg2]]
        ...
        ^:ignore-warnings [::injected-dynamic arg1 arg2]))]
   (fn [{:as cofx {::keys [injected-dynamic]} [_ event-arg1 event-arg-2]]
     ...)
  "
  [query-vector-or-event->query-vector-fn]
  {:pre [(or (vector? query-vector-or-event->query-vector-fn)
             (ifn? query-vector-or-event->query-vector-fn))]}
  (re-frame/inject-cofx
   :sub
   query-vector-or-event->query-vector-fn))

(re-frame/reg-cofx :sub inject-sub-cofx)

;;
;; * Dispatch event on every sub value
;;

(defonce track-register (atom {}))

(defn new-track
  "Create a new reagent track that will execute every time `subscription`
  updates.

  If `event` is provided will always dispatch that event.

  If event is nil, `val->event` is required and will be invoked with the latest
  value from the subscription. It should return an event to dispatch or nil for
  a no-op.

  "
  [{:keys [subscription event val->event]}]
  {:pre [(vector? subscription) (or (vector? event) (ifn? val->event))]}
  #?(:cljs
     (ratom/track!
      (fn []
        (let [val @(re-frame/subscribe subscription)]
          (when-some [event (or event (val->event val))]
            (re-frame/dispatch event)))))))

(defn ensure-vec [x] (if (sequential? x) (vec x) (vector x)))

(defmulti track-fx* :action)

(defmethod track-fx* :register
  [{:keys [id] :as track}]
  (if-some [track' (get @track-register id)]
    (throw (ex-info "Track already exists" {:track track' :tried track}))
    (let [track (new-track track)]
      (swap! track-register assoc id track))))

(defmethod track-fx* :dispose
  [{:keys [id] :as track}]
  (if-some [track (get @track-register id)]
    #?(:cljs (do (ratom/dispose! track) (swap! track-register dissoc id)) :clj nil)
    (throw (ex-info "Track isn't registered" {:track track}))))

(defn track-fx
  [track-or-tracks]
  (doseq [track (ensure-vec track-or-tracks)]
    (track-fx* track)))

(re-frame/reg-fx :track track-fx)

(comment
  (do
    (require '[re-frame.interop :as interop])
    (re-frame/reg-event-fx
     ::start-trigger-track
     (fn [cofx event]
       {:track
        {:action       :register
         :id           42
         :subscription [::query-vector  "blah"]
         :val->event   (fn [val] [::trigger val])}}))

    (re-frame/reg-event-fx
     ::stop-trigger-track
     (fn [cofx event]
       {:track
        {:action :dispose
         :id     42}}))
    ;; Define a sub and the event we want to trigger
    (defonce foo (interop/ratom 0))
    (re-frame/reg-sub-raw ::query-vector (fn [_ _] (interop/make-reaction #(deref foo))))
    (re-frame/reg-event-db ::trigger (fn [db val] (println "Trigger" val) db))
    ;; Start the track
    (re-frame/dispatch [::start-trigger-track])
    ;; Update the ::query-vector, will cause ::trigger to run
    (swap! foo inc)
    (swap! foo inc)
    (swap! foo inc)
    ;; Stop the track, updates to ::query-vector aren't tracked anymore
    (re-frame/dispatch [::stop-trigger-track])
    (swap! foo inc)))

;;
;; * Scheduler
;;

;; Register in this file so we don't need to require scheduler

(re-frame/reg-fx :scheduler scheduler/scheduler-fx)

;;
;; * UUID fn
;;

(defn uuid-fn
  [context _]
  (assoc context :uuid-fn uuid/uuid))

(re-frame/reg-cofx :uuid-fn uuid-fn)

;;
;; * Timestamp
;;

(defn timestamp
  [context _]
  (assoc context :timestamp (util/inst)))

(re-frame/reg-cofx :timestamp timestamp)

;;
;; * Time elapsed between two events
;;

(defonce elapsed-register (atom {}))

(defn elapsed
  [context [id :as event]]
  (let [now  (util/now)
        prev (get @elapsed-register id -1)]
    (swap! elapsed-register assoc id now)
    (assoc context :elapsed (if (neg? prev) -1 (- now prev)))))

(re-frame/reg-cofx :elapsed elapsed)
