(ns vimsical.frontend.util.re-frame
  (:require [clojure.spec :as s]))

(defn <sub
  [sub]
  (deref (re-frame.core/subscribe sub)))

(defn- rewrite-query? [qexpr]
  (and (= 2 (count qexpr))
       (let [[?link ?pattern] qexpr]
         (and (keyword? ?link)
              (vector? ?pattern)))))

(defn- rewrite-link-query [[link pattern]]
  {:link    link
   :pattern [{[link '_] pattern}]})

(defn <sub-query [qexpr]
  (if (rewrite-query? qexpr)
    (let [{:keys [link pattern]} (rewrite-link-query qexpr)]
      (get (<sub [:q pattern]) link))
    (<sub [:q qexpr])))

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