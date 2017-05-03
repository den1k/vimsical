(ns vimsical.frontend.util.re-frame
  (:require [clojure.spec :as s]
            [vimsical.frontend.db :as db]))

(defn <sub
  [sub]
  (deref (re-frame.core/subscribe sub)))

#?(:cljs
   (defn <sub-query [qexpr]
     (if (db/rewrite-query? qexpr)
       (let [{:keys [link pattern]} (db/rewrite-link-query qexpr)]
         (get (<sub [:q pattern]) link))
       (<sub [:q qexpr]))))

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