(ns vimsical.frontend.util.scheduler
  "Simple dispatch scheduler for re-frame."
  (:require
   [clojure.spec :as s]
   [clojure.data.avl :as avl]
   [re-frame.core :as re-frame]))

;;
;; * Protocol
;;

(defprotocol IScheduler
  (running? [_])
  (start! [_ t tick])
  (pause! [_])
  (stop! [_])
  (set-time! [_ t]))

(defprotocol ITimeScheduler
  (schedule! [_ t event override?]))

(defprotocol ISchedulerInternal
  (request-tick! [_])
  (tick! [_ now] "Callback for requestAnimationFrame"))

;;
;; * Interop
;;

#?(:cljs
   (defn- now [] (.now js/window.performance))
   :clj
   (let [started-at (System/nanoTime)]
     (defn now []
       (double
        (/ (- (System/nanoTime) started-at) 1000000.0)))))

(defn request-tick*
  [tick-fn]
  #?(:cljs (.requestAnimationFrame js/window tick-fn)
     :clj  (let [delay 16
                 cb    (fn [] (Thread/sleep delay) (tick-fn))]
             (doto (Thread. cb) (.start)))))

(defn cancel-tick*
  [tick-id]
  #?(:cljs (.cancelAnimationFrame js/window tick-id)
     :clj  (.interrupt ^Thread tick-id)))

(defn- ticker [scheduler]
  (fn ticker-cb
    ([] (tick! scheduler (now)))
    ([now] (tick! scheduler now))))

;;
;; * Time scheduler
;;

;; 1. We might miss some time->event if multiple times happen with one tick loop. Use
;; avl/split-key to get all the time->event to the left

;; 2. extend api to pair times with time->event

(defn events-before
  [m t]
  (when-some [[k] (avl/nearest m <= t)]
    (let [[l e r] (avl/split-key k m)]
      [(seq (vals (conj l e))) r])))

(deftype TimeScheduler
    [#?(:cljs ^:mutable tick     :clj ^:unsynchronized-mutable tick)
     #?(:cljs ^:mutable running     :clj ^:unsynchronized-mutable running)
     #?(:cljs ^:mutable started-at  :clj ^:unsynchronized-mutable started-at)
     #?(:cljs ^:mutable time->event :clj ^:unsynchronized-mutable time->event)
     #?(:cljs ^:mutable tick-id     :clj ^:unsynchronized-mutable tick-id)]
  ISchedulerInternal
  (request-tick! [this]
    (set! tick-id (request-tick* (ticker this))))
  (tick! [this now]
    (when running
      (let [elapsed (- now started-at)]
        (when tick (tick elapsed))
        (when-some [[events next-time->event] (events-before time->event elapsed)]
          (doseq [event events] (re-frame/dispatch event))
          (set! time->event next-time->event)))
      (request-tick! this)))

  IScheduler
  (running? [_] running)
  (start! [this t tick']
    (set-time! this t)
    (set! tick tick')
    (set! running true)
    (request-tick! this))
  (pause! [this]
    (set! running false))
  (stop! [this]
    (when-not running (throw (ex-info "Scheduler not running" {})))
    (cancel-tick* tick-id)
    (when tick (tick nil))
    (set! started-at nil)
    (set! time->event (empty time->event)))
  (set-time! [this t]
    (let [started-at' (cond-> (now) (some? t) (- t))]
      (set! started-at started-at')))

  ITimeScheduler
  (schedule! [this t event override?]
    (when (and t event)
      (let [time->event' (cond-> time->event override? (empty))]
        (set! time->event (assoc time->event' t event))))))

(defn scheduler? [x] (and x (instance? TimeScheduler x)))

(defn new-scheduler
  [] (->TimeScheduler nil false nil (avl/sorted-map) nil))

(comment
  (do
    (require '[re-frame.interop :as interop])
    (re-frame/reg-event-db :foo (fn [db v] (println :foo v) db))
    (def elapsed (interop/ratom nil))
    (def s (new-scheduler (partial reset! elapsed)))
    (schedule! s 3000 [:foo])
    (schedule! s 2000 [:foo])
    (schedule! s 1000 [:foo])
    (start! s)
    ;; (pause! s)
    (deref elapsed)
    #_(stop! s)))

;;
;; * Cofx & Fx
;;

(s/def ::t number?)
(s/def ::event vector?)
(s/def ::tick ifn?)
(s/def ::tick ifn?)
(s/def ::override? boolean?)

(s/def ::start (s/keys :req-un [::t] :opt-un [::tick]))
(s/def ::pause any?)
(s/def ::stop any?)
(s/def ::set-time (s/keys :req-un [::t]))
(s/def ::schedule (s/keys :req-un [::t ::event] :opt-un [::override?]))

(s/def ::scheduler-actions
  (s/or :start ::start
        :pause ::pause
        :stop ::stop
        :set-time ::set-time
        :schedule ::schedule))

(defonce scheduler (new-scheduler))

(defmulti perform-action! :action)

(defmethod perform-action! nil [_])
(defmethod perform-action! :start    [{:keys [t tick]}]  (start! scheduler t tick))
(defmethod perform-action! :pause    [{:keys [tick]}]    (pause! scheduler))
(defmethod perform-action! :stop     [{:keys [tick]}]    (stop! scheduler))
(defmethod perform-action! :set-time [{:keys [t]}]       (set-time! scheduler t))
(defmethod perform-action! :schedule [{:keys [t event override?]}]
  (schedule! scheduler t event override?))

(s/def ::scheduler-fx
  (s/or :one ::scheduler-actions
        :many (s/every ::scheduler-actions)))

(s/fdef scheduler-fx :args (s/cat :fx ::scheduler-fx))

(defn scheduler-fx
  [one-or-many]
  (letfn [(ensure-seq [x]
            (if (map? x) [x] x))]
    (doseq [{:keys [t event override? action] :as sched} (ensure-seq one-or-many)]
      (when action (println "Scheduler:" action))
      (perform-action! sched))))
