#?(:clj
   (require
    '[figwheel-sidecar.repl :as r]
    '[figwheel-sidecar.repl-api :as ra]
    '[figwheel-sidecar.config :as config]))

#?(:clj
   (def figwheel-config
     {:figwheel-options {:css-dirs ["resources/public/css"]}
      :build-ids        ["dev"]
      :all-builds       (config/get-project-builds)}))

#?(:clj
   (defn start-dev
     []
     (println "Starting client dev build with figwheel...")
     (ra/start-figwheel! figwheel-config)
     (ra/cljs-repl)))

#?(:clj
   (start-dev))

#?(:cljs
   (require '[vimsical.util.dev]))
