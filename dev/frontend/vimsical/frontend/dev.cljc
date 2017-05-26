(ns vimsical.frontend.dev
  (:require
   [vimsical.frontend.config :as config]
   #?@(:cljs [[vimsical.frontend.core :as core]
              [re-frisk.core :refer [enable-re-frisk!]]])))

#?(:cljs
   (do
     (enable-console-print!)
     (enable-re-frisk!)
     (println "dev mode")))

#?(:cljs
   (defn on-reload
     "Called by figwheel on reload. See project.clj."
     []
     (core/mount-root)))

;;
;; * Dev Utils
;;

(defn entity-namespace? [entity-key-or-namespace entity]
  (letfn [(kw-ns [kw] (-> kw namespace keyword))]
    (boolean
     (some-> (dissoc entity :db/uid)
             ffirst
             kw-ns
             (= (or (kw-ns entity-key-or-namespace) entity-key-or-namespace))))))

(defn check-entity [namespace entity]
  #?(:cljs
     (when config/debug?
       (when-not (entity-namespace? namespace entity)
         (js/console.error
          (str "Expected entity with namespace: " namespace)
          {:namespace namespace
           :entity    entity})
         (js-debugger)))))