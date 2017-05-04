(ns vimsical.frontend.util.re-frame
  (:require
   [clojure.spec :as s]
   [clojure.set :as set]
   [re-frame.core :as re-frame]
   #?(:cljs [vimsical.frontend.util.scheduler :as util.scheduler])
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
;; * Inject sub
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
       (ratom/dispose! ratom-or-reaction))))

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
;; * Scheduler
;;


(defonce schedulers-by-event (atom {}))

(s/def ::ms nat-int?)
(s/def ::scheduler #?(:cljs util.scheduler/scheduler? :clj any?))
(s/def ::scheduler-fx-one (s/keys :req-un [::scheduler ::ms]))
(s/def ::scheduler-fx-many (s/every ::scheduler-fx-one))
(s/def ::scheduler-fx (s/or :one ::scheduler-fx-one :many ::scheduler-fx-many))

(defn- get-or-create-scheduler [event]
  #?(:cljs
     (or (get @schedulers-by-event event)
         (let [scheduler (util.scheduler/new-scheduler event)]
           (swap! schedulers-by-event assoc event scheduler)
           scheduler))))

(re-frame/reg-cofx
 :scheduler
 (fn [cofx [id :as event]]
   (assoc cofx :scheduler (get-or-create-scheduler event))))

(s/fdef scheduler-fx
        :args (s/cat :fx ::scheduler-fx)
        :ret any?)

(defn scheduler-fx
  [one-or-many]
  #?(:cljs
     (doseq [{:keys [scheduler ms] :as effect} (if (map? one-or-many) [one-or-many] one-or-many)]
       (util.scheduler/schedule-delay! scheduler ms))
     :clj
     (throw (ex-info "Scheduler not implemented on the JVM" {}))))

(re-frame/reg-fx :schedule scheduler-fx)
