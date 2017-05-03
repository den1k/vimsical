(ns vimsical.frontend.util.re-frame
  (:require
   [clojure.set :as set]
   [re-frame.core :as re-frame]
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
;; * Interceptors
;;

;; ** Inject sub
;; *** Subs disposal helpers

(defprotocol WalkDisposable
  (-dispose-maybe [_ watches-set]))

(defn dispose-maybe
  [ratom-or-reaction]
  (-dispose-maybe ratom-or-reaction #{}))

#?(:clj
   ;; No-op with the clj interop
   (extend-protocol WalkDisposable Object (-dispose-maybe [_ _]))
   :cljs
   (extend-protocol WalkDisposable
     ratom/RAtom
     (-dispose-maybe [ratom watches-set]
       (let [watches  (.-watches ratom)]
         (when (set/superset? watches-set (set watches))
           (ratom/dispose! ratom))))
     ratom/Reaction
     (-dispose-maybe [reaction watches-set]
       (let [watches  (.-watches reaction)]
         (when (set/superset? watches-set (set watches))
           (ratom/dispose! reaction))
         (doseq [w (.-watching reaction)]
           (-dispose-maybe w (conj watches-set reaction)))))))

;; *** Co-fx injector

(re-frame/reg-cofx
 :sub
 (fn [cofx [id :as query-vector]]
   (let [sub (re-frame/subscribe query-vector)
         val (deref sub)]
     (-dispose-maybe sub #{})
     (assoc cofx id val))))

(defn inject-sub
  [query-vector]
  (re-frame/inject-cofx :sub query-vector))

(defn subscribe-once
  [query-vector]
  (dispose-maybe (re-frame/subscribe query-vector)))
