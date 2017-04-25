(ns vimsical.frontend.util.dom
  "Util.dom is meant to be used exclusively in the browser. To avoid
  double reloads of .clj style fyles through figwheel, we made this a .cljc file."
  (:require [vimsical.common.util.core :as util]))

#?(:clj
   (defmacro e->
     "Event handler macro. Wraps clojure.core/-> into an fn that takes e and
     threads it through exprs. Similar to comp, but doesn't require forms to be
     functions."
     [& body]
     `(fn e-># [e#]
        (-> e#
            ~@body))))

#?(:clj
   (defmacro e->>
     "Event handler macro. Wraps clojure.core/->> into an fn that takes e and
     threads it through exprs. Similar to comp, but doesn't require forms to be
     functions."
     [& body]
     `(fn e->># [e#]
        (->> e#
             ~@body))))

#?(:clj
   (defmacro e>
     "Event handler macro. Makes e (event) and target available in the body."
     ([& body]
      `(fn e># [~'e]
         (let [~'target (.-target ~'e)
               ~'value (.-value ~'target)
               ~'inner-html (.-innerHTML ~'target)]
           ~@body)))))