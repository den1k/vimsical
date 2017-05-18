(ns vimsical.backend.components.server.interceptors.deps
  (:require
   [io.pedestal.interceptor :as interceptor]
   [vimsical.backend.components.server.interceptors.spec :as interceptors.spec]
   [clojure.spec :as s]))

;;
;; * Dependencies injection
;;

(defn new-context-dependencies-injector
  "Create a before interceptor that will merge the dependencies of `component`
  into the context map."
  [component & ks]
  (let [deps (not-empty (select-keys component ks))]
    (interceptor/interceptor
     {:name  ::deps
      :enter (fn [context] (merge context deps))})))
