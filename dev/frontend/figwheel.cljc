;; Figwheel start script
;; run with:

;; lein with-profile frontend-dev \
;; run -m clojure.main dev/frontend/figwheel.cljc <build-id>
;; build-id can be "dev" or "dev-player" (see project.clj)

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
   (def build-id
     "Takes command line args as build ids."
     (or (first *command-line-args*) "dev")))

#?(:clj (def port
          (case build-id
            "dev"        3449
            "player-dev" 3450)))

#?(:clj
   (def config {:build-id build-id :port port}))

#?(:clj
   (def figwheel-config
     (let [{:keys [build-id port]} config]
       {:figwheel-options {:css-dirs         ["resources/public/css"]
                           :reload-clj-files {:clj false :cljc true}
                           :server-port      port}
        :build-ids        [build-id]
        :all-builds       cljs-builds})))

#?(:clj
   (defn start-dev
     []
     (println "Starting client dev build with figwheel...")
     (ra/start-figwheel! figwheel-config)
     (ra/cljs-repl)))

#?(:clj
   (start-dev))
