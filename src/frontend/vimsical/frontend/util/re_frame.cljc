(ns vimsical.frontend.util.re-frame
  (:require
   [clojure.spec :as s]
   [clojure.set :as set]
   [re-frame.core :as re-frame]
   [re-frame.interop :as interop]
   [vimsical.frontend.util.scheduler :as util.scheduler]
   #?(:cljs [reagent.ratom :as ratom])))

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

(defn dispose-maybe
  "Dispose of `ratom-or-reaction` if it has no watches."
  [ratom-or-reaction]
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
       (interop/dispose! ratom-or-reaction))))

(re-frame/reg-cofx
 :sub
 (fn [cofx [id :as query-vector]]
   (let [sub (re-frame/subscribe query-vector)
         val (deref sub)]
     (dispose-maybe sub)
     (assoc cofx id val))))

(defn inject-sub
  [query-vector]
  (re-frame/inject-cofx :sub query-vector))

(defn subscribe-once
  [query-vector]
  (dispose-maybe (re-frame/subscribe query-vector)))

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

(defmulti track-fx :action)

(defmethod track-fx :register
  [{:keys [id] :as track}]
  (if-some [track (get @track-register id)]
    (throw (ex-info "Track already exists" {:track track}))
    (let [track (new-track track)]
      (swap! track-register assoc id track))))

(defmethod track-fx :dispose
  [{:keys [id] :as track}]
  (if-some [track (get @track-register id)]
    #?(:cljs (do (ratom/dispose! track) (swap! track-register dissoc id)) :clj nil)
    (throw (ex-info "Track isn't registered" {:track track}))))

(re-frame/reg-fx :track track-fx)

(comment
  (do
    (re-frame/reg-event-fx
     ::start-trigger-track
     (fn [cofx event]
       {:track
        {:action       :register
         :id           42
         :subscription [::sub  "blah"]
         :val->event   (fn [val] [::trigger val])}}))

    (re-frame/reg-event-fx
     ::stop-trigger-track
     (fn [cofx event]
       {:track
        {:action :dispose
         :id     42}}))
    ;; Define a sub and the event we want to trigger
    (defonce foo (ratom/atom 0))
    (re-frame/reg-sub-raw ::sub (fn [_ _] (ratom/make-reaction #(deref foo))))
    (re-frame/reg-event-db ::trigger (fn [db val] (println "Trigger" val) db))
    ;; Start the track
    (re-frame/dispatch [::start-trigger-track])
    ;; Update the ::sub, will cause ::trigger to run
    (swap! foo inc)
    (swap! foo inc)
    (swap! foo inc)
    ;; Stop the track, updates to ::sub aren't tracked anymore
    (re-frame/dispatch [::stop-trigger-track])
    (swap! foo inc)))
