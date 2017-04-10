(defproject vimsical "0.1.0-SNAPSHOT"
  :dependencies
  [[org.clojure/clojure "1.9.0-alpha15"]]

  :source-paths []                                          ; ignore src/ in all profiles

  :clean-targets
  ^{:protect false}
  ["resources/public/js/compiled" "target"]

  :profiles
  {:dev
   {:plugins
    [[lein-pprint "1.1.2"]                                  ; lein with-profile frontend-dev pprint
     [lein-environ "1.1.0"]]}

   :test
   [:backend :frontend :vcs :common
    {:source-paths
     ["test/backend" "test/frontend" "test/vcs" "test/common"]
     :dependencies
     [[org.clojure/test.check "0.9.0"]]}]

   :uberjar
   {:aot          :all
    :omit-source  true
    :uberjar-name "vimsical.jar"}

   ;;
   ;; Vcs
   ;;
   :vcs
   {:source-paths ["src/vcs"]
    :dependencies []}

   ;;
   ;; Common
   ;;
   :common
   {:source-paths
    ["src/common"]
    :dependencies
    [[com.cognitect/transit-cljs "0.8.239"]
     [org.clojure/core.async "0.3.442"]
     [com.stuartsierra/component "0.3.1"]
     [medley "0.8.4"]
     [environ "1.1.0"]]}

   ;;
   ;; Backend
   ;;
   :backend
   [:vcs :common
    {:source-paths
     ["src/backend"]
     :repositories
     {"my.datomic.com"
      {:url      "https://my.datomic.com/repo"
       :username :env/datomic_login
       :password :env/datomic_password}}
     :dependencies
     [[com.taoensso/carmine "2.15.0"]
      [org.immutant/web "2.1.5" :exclusions [ring/ring-core]]
      [cc.qbits/alia-all "3.3.0"]
      [com.datomic/datomic-pro "0.9.5544" :exclusions [commons-codec]]]
     :main
     vimsical.backend.core}]

   :backend-dev
   [:backend
    {:dependencies
     [[criterium "0.4.4"]]
     :source-paths
     ["dev/backend"]}]

   ;;
   ;; Frontend
   ;;
   :frontend
   [:vcs :common :cljs
    {:source-paths
     ["src/frontend" "src/styles"]
     :plugins
     [[lein-cljsbuild "1.1.4"
       :exclusions [org.apache.commons/commons-compress]]
      [lein-garden "0.2.8"
       :exclusions [org.clojure/clojure]]]
     :dependencies
     [[org.clojure/clojurescript "1.9.494"]
      [cljsjs/codemirror "5.11.0-2"]
      [garden "1.3.2"]
      ;; Added this to fix a compilation issue with garden
      [ns-tracker "0.3.0"]
      [cljsjs/google-diff-match-patch "20121119-1"]
      [com.stuartsierra/mapgraph "0.2.1"]
      [reagent "0.6.1"]
      [re-frame "0.9.2"]
      [re-com "0.9.0"]]
     :prep-tasks
     [["garden" "once"]]
     :garden
     {:builds
      [{:id           "dev-styles"
        :source-paths ["src/frontend"]
        :stylesheet   vimsical.frontend.styles/styles
        :compiler     {:output-to     "resources/public/css/app.css"
                       :vendors       ["webkit" "moz"]
                       :auto-prefix   #{:user-select}
                       :pretty-print? true}}]}}]

   :frontend-dev
   [:frontend
    {:plugins
     [[lein-figwheel "0.5.9"]]
     :dependencies
     [[com.cemerick/piggieback "0.2.2-SNAPSHOT"]
      [figwheel-sidecar "0.5.10-SNAPSHOT"]
      [binaryage/devtools "0.8.3"]]
     :source-paths
     ["dev/frontend"]
     :repl-options
     {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
     :figwheel
     {:css-dirs ["resources/public/css"]}}]

   ;;
   ;; Cljs
   ;;
   :cljs
   {:cljsbuild
    {:builds
     [{:id           "prod"
       :jar          true
       :source-paths ["src/frontend" "src/common" "src/vcs"]
       :compiler     {:main            vimsical.frontend.core
                      :asset-path      "/js"
                      :externs         ["resources/externs/svg.js"]
                      :output-to       "resources/public/js/compiled/vimsical.js"
                      :optimizations   :advanced
                      :pretty-print    false
                      :parallel-build  true
                      :closure-defines {goog.DEBUG false}
                      ;; Determines whether readable names are emitted. This can
                      ;; be useful when debugging issues in the optimized
                      ;; JavaScript and can aid in finding missing
                      ;; externs. Defaults to false.
                      :pseudo-names    false}}
      {:id               "dev"
       :figwheel         {:on-jsload vimsical.frontend.core/on-reload}
       :source-paths     ["src/frontend" "src/common" "src/vcs" "dev/frontend"]
       :compiler         {:main                 vimsical.frontend.core
                          :asset-path           "/js/compiled/out"
                          :output-to            "resources/public/js/compiled/vimsical.js"
                          :output-dir           "resources/public/js/compiled/out"
                          :optimizations        :none
                          :parallel-build       true
                          ;; Add cache busting timestamps to source map urls.
                          ;; This is helpful for keeping source maps up to date when
                          ;; live reloading code.
                          :source-map-timestamp true
                          :preloads             [devtools.preload]
                          :external-config      {:devtools/config
                                                 {:features-to-install :all
                                                  :fn-symbol           "λ"}}}}]}}})
