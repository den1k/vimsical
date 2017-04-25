(ns vimsical.frontend.util.re-frame)

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

#?(:clj
   (defn- link-joins->query [[link joins]]
     [:q*
      joins
      link]))

#?(:clj
   (defn- binding-link-joins->query [[binding link-joins]]
     [binding (link-joins->query link-joins)]))

#?(:clj
   (defmacro with-queries
     [bindings & body]
     `(let [~@(apply concat (map (comp binding-sub-deref
                                       binding-link-joins->query)
                                 (partition 2 bindings)))]
        ~@body)))


#_(macroexpand
   '(with-queries [app-user [:app/user [:user/first-name
                                        {:user/vimsae [:vims/title]}]]]))