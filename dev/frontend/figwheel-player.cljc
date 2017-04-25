#?(:clj
   (require
    '[figwheel-sidecar.repl :as r]
    '[figwheel-sidecar.repl-api :as ra]
    '[figwheel-sidecar.config :as config]))

#?(:clj
   ;; This fetches :cljsbuilds from project.clj via the :cljs profile
   (def cljs-builds
     (-> (config/get-project-config) :profiles :cljs :cljsbuild :builds)))

#?(:clj
   (def figwheel-config
     ;; todo separate resource path for player
     {:figwheel-options {:css-dirs         ["resources/public/css"]
                         :reload-clj-files {:clj false :cljc true}
                         :server-port      3450}
      :build-ids        ["player-dev"]
      :all-builds       cljs-builds}))

#?(:clj
   (defn start-dev
     []
     (println "Starting client player dev build with figwheel...")
     (ra/start-figwheel! figwheel-config)
     (ra/cljs-repl)))

#?(:clj
   (start-dev))