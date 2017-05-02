(ns vimsical.frontend.util.re-frame
  (:require [re-frame.core :as re-frame]))

(defn <sub
  [sub]
  (deref (re-frame/subscribe sub)))

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
