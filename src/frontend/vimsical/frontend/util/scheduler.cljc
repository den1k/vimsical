(ns vimsical.frontend.util.scheduler
  "Simple dispatch scheduler for re-frame."
  (:require
   [clojure.data.avl :as avl]
   [re-frame.core :as re-frame])
  #?(:cljs (:import (goog.async.nextTick))))

;;
;; * Protocol
;;

(defprotocol IScheduler
  (running? [_])
  (start! [_])
  (pause! [_])
  (stop! [_]))

(defprotocol ITimeScheduler
  (schedule-time! [_ t]))

(defprotocol ISchedulerInternal
  (request-tick! [_])
  (tick! [_ now] "Callback for requestAnimationFrame")
  (fire! [_]))

;;
;; * Interop
;;

#?(:cljs
   (defn- now [] (.now js/window.performance))
   :clj
   (let [started-at (System/nanoTime)]
     (defn now []
       (- (System/nanoTime) started-at))))

(defn request-callback!
  [tick-fn]
  #?(:cljs (.requestAnimationFrame js/window tick-fn)
     :clj  (let [delay 16
                 cb    (fn [] (Thread/sleep delay) (tick-fn))]
             (doto (Thread. cb) (.start)))))

(defn cancel-callback!
  [id]
  #?(:cljs (.cancelAnimationFrame js/window id)
     :clj  (.interrupt ^Thread id)))

(defn- ticker [scheduler]
  (fn ticker-cb
    ([] (tick! scheduler (now)))
    ([now] (tick! scheduler now))))

;;
;; * Time scheduler
;;

(deftype TimeScheduler
    [event
     elapsed-atom
     #?(:cljs ^:mutable running    :clj ^:unsynchronized-mutable running )
     #?(:cljs ^:mutable started-at :clj ^:unsynchronized-mutable started-at)
     #?(:cljs ^:mutable next-time  :clj ^:unsynchronized-mutable next-time)
     #?(:cljs ^:mutable times      :clj ^:unsynchronized-mutable times)
     #?(:cljs ^:mutable id         :clj ^:unsynchronized-mutable id)]
  ISchedulerInternal
  (request-tick! [this]
    ;; Register the callback id so we can cancel it
    (set! id (request-callback! (ticker this))))
  (tick! [this now]
    (when running
      (let [elapsed (- now started-at)]
        (when elapsed-atom (reset! elapsed-atom elapsed))
        (when (and next-time (<= next-time elapsed)) (fire! this)))
      (request-tick! this)))
  (fire! [this]
    (re-frame/dispatch event)
    ;; Shift the times by 1 element to the right, resetting the next-time
    (let [[next-time' :as times'] (disj times next-time)]
      (set! times times')
      (set! next-time next-time')))

  IScheduler
  (running? [_] running)
  (start! [this]
    (set! started-at (now))
    (set! running true)
    (request-tick! this))
  (pause! [this]
    (set! running false))
  (stop! [this]
    (when-not running (throw (ex-info "Scheduler not running" {})))
    (set! next-time nil)
    (cancel-callback! id)
    (set! started-at nil)
    (set! times (empty times)))

  ITimeScheduler
  (schedule-time! [this t]
    (let [[next-time' :as times'] (conj times t)]
      ;; Make sure to reset the next-time to the first element in times, in case
      ;; the new delay appears before what's currently our next-time
      (set! times times')
      (set! next-time next-time'))))

(defn scheduler? [x] (and x (instance? TimeScheduler x)))

(defn new-scheduler
  ([event] (new-scheduler event nil))
  ([event elapsed-atom]
   {:pre [(vector? event)]}
   (TimeScheduler. event elapsed-atom false nil nil (avl/sorted-set) nil)))

(comment
  (do
    (require '[re-frame.interop :as interop])
    (re-frame/reg-event-db :foo (fn [db v] (println :foo v) db))
    (def elapsed (interop/ratom nil))
    (def s (new-scheduler [:foo]  elapsed))
    (schedule-time! s 3000)
    (schedule-time! s 2000)
    (schedule-time! s 1000)
    (start! s)
    ;; (pause! s)
    (deref elapsed)
    #_(stop! s)))

;;
;; * Cofx & Fx
;;

(defonce schedulers-by-event (atom {}))

(defn- get-or-create-scheduler [f & [event :as args]]
  (or (get @schedulers-by-event event)
      (let [scheduler (apply f args)]
        (swap! schedulers-by-event assoc event scheduler)
        scheduler)))

(re-frame/reg-cofx
 :scheduler
 (fn [cofx [event elapsed-atom]]
   (assoc cofx :scheduler (get-or-create-scheduler new-scheduler event elapsed-atom))))

(defn scheduler-fx
  [one-or-many]
  (doseq [{:keys [scheduler t action]} (if (map? one-or-many) [one-or-many] one-or-many)]
    (case action
      :start (start! scheduler)
      :stop  (stop! scheduler)
      :pause (pause! scheduler)
      nil)
    (when t (schedule-time! scheduler t))))

(re-frame/reg-fx :schedule scheduler-fx)
