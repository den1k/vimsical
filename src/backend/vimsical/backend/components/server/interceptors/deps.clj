(ns vimsical.backend.components.server.interceptors.deps
  (:require
   [io.pedestal.interceptor :as interceptor]
   [com.stuartsierra.component :as cp]))

;;
;; * Dependencies injection
;;

(defn new-context-dependencies-injector
  "Create a before interceptor that will merge the dependencies of `component`
  into the context map."
  [component]
  (let [deps (cp/dependencies component)]
    (interceptor/interceptor
     {:name  ::deps
      :enter (fn [context] (merge context deps))})))
