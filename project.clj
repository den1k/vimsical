(defproject vimsical "0.1.0-SNAPSHOT"
  :dependencies
  [[org.clojure/clojure "1.9.0-alpha15"]]

  :source-paths []                      ; ignore src/ in all profiles

  :clean-targets
  ^{:protect false}
  ["resources/public/js/compiled" "target"]

  :profiles
  {:dev
   {:plugins
    [[lein-pprint "1.1.2"]              ; lein with-profile frontend-dev pprint
     [lein-environ "1.1.0"]]}

   :test
   {:dependencies
    [[org.clojure/test.check "0.9.0"]]}

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
     [org.clojure/core.async "0.3.442" :exclusions [org.clojure/tools.reader]]
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

   :backend-test
   [:test :backend :vcs :common
    {:source-paths
     ["test/backend" "test/vcs" "test/common"]
     :dependencies
     [[org.clojure/test.check "0.9.0"]]}]
   ;;
   ;; Frontend
   ;;
   :frontend
   [:vcs :common :cljs :css
    {:source-paths
     ["src/frontend"]
     :plugins
     [[lein-cljsbuild "1.1.5"
       :exclusions [org.apache.commons/commons-compress]]]
     :dependencies
     [[org.clojure/clojurescript "1.9.518"]
      ;; Dependency of Google Closure compiler
      [com.google.guava/guava "21.0"]
      [cljsjs/google-diff-match-patch "20121119-1"]
      ;; Our mapgraph fork. Must be be symlinked in checkouts.
      [com.stuartsierra/mapgraph "0.2.2-SNAPSHOT" :exclusions [org.clojure/clojure re-frame]]
      [reagent "0.6.1" :exclusions [org.clojure/clojurescript]]
      [re-frame "0.9.2" :exclusions [org.clojure/clojurescript]]
      [re-com "2.0.0" :exclusions [reagent org.clojure/clojurescript org.clojure/core.async]]
      [thi.ng/color "1.2.0"]]}]

   :frontend-dev
   [:frontend
    {:plugins
     [[lein-figwheel "0.5.9" :exclusions [[org.clojure/clojure]]]]
     :dependencies
     [[com.cemerick/piggieback "0.2.2-SNAPSHOT"]
      [figwheel-sidecar "0.5.10" :exclusions [org.clojure/clojurescript]]
      [re-frisk "0.4.4" :exclusions [re-frame org.clojure/clojurescript]]
      ;; needed as a dep for re-frame.trace
      [binaryage/devtools "0.8.3"]
      ;; re-frame.trace - clone and install to use
      ;; https://github.com/Day8/re-frame-trace
      [day8.re-frame/abra "0.0.9-SNAPSHOT" :exclusions [re-frame reagent org.clojure/clojurescript]]
      [org.clojure/tools.nrepl "0.2.13"]]
     :source-paths
     ["dev/frontend"]
     :repl-options
     {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
     :figwheel
     {:css-dirs ["resources/public/css"]}}]

   :frontend-test
   [:test :frontend :vcs :common
    {:source-paths
     ["test/frontend" "test/vcs" "test/common"]
     :plugins [[lein-doo "0.1.7"]]}]

   ;;
   ;; CSS
   ;;
   ;; NOTE this profile won't run by itself since it needs some frontend deps
   :css
   {:plugins      [[lein-garden "0.2.8" :exclusions [org.clojure/clojure]]]
    :dependencies [[garden "1.3.2"]
                   ;; Added this to fix a compilation issue with garden
                   [ns-tracker "0.3.0"]]
    :prep-tasks   [["garden" "once"]]
    :garden
    {:builds
     [{:id           "dev-styles"
       :source-paths ["src/frontend" "src/common"]
       :stylesheet   vimsical.frontend.styles.core/styles
       :compiler     {:output-to     "resources/public/css/app.css"
                      :vendors       ["webkit" "moz"]
                      :auto-prefix   #{:user-select}
                      :pretty-print? true}}]}}

   ;;
   ;; Cljs
   ;;
   :cljs
   {:cljsbuild
    {:builds
     [{:id           "prod"
       :jar          true
       :source-paths ["checkouts/mapgraph/src" "src/frontend" "src/common" "src/vcs"]
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
      {:id           "dev"
       :figwheel     {:on-jsload vimsical.frontend.dev/on-reload}
       :source-paths ["checkouts/mapgraph/src" "dev/frontend" "src/frontend" "src/common" "src/vcs" "dev/frontend"]
       :compiler     {:main                 vimsical.frontend.core
                      :asset-path           "/js/compiled/out"
                      :output-to            "resources/public/js/compiled/vimsical.js"
                      :output-dir           "resources/public/js/compiled/out"
                      :optimizations        :none
                      :parallel-build       true
                      ;; Add cache busting timestamps to source map urls.
                      ;; This is helpful for keeping source maps up to date when
                      ;; live reloading code.
                      :source-map-timestamp true
                      :closure-defines      {goog.DEBUG                          true
                                             re_frame.trace.trace_enabled_QMARK_ true}
                      :preloads             [devtools.preload
                                             day8.re-frame.trace.preload]

                      :external-config      {:devtools/config
                                             {:features-to-install :all
                                              :fn-symbol           "λ"}}}}
      {:id           "test"
       :source-paths ["checkouts/mapgraph/src" "dev/frontend" "src/frontend" "src/common" "src/vcs" "test/frontend" "test/common" "test/vcs" "test/runner"]
       :compiler     {:output-to      "resources/public/js/compiled/vimsical-test.js"
                      :output-dir     "resources/public/js/compiled/out-test"
                      :main           vimsical.runner
                      :target         :nodejs
                      :optimizations  :none
                      :parallel-build true}}
      {:id           "test-advanced"
       :source-paths ["checkouts/mapgraph/src" "dev/frontend" "src/frontend" "src/common" "src/vcs" "test/frontend" "test/common" "test/vcs" "test/runner"]
       :compiler     {:output-to      "resources/public/js/compiled/vimsical-test.js"
                      :output-dir     "resources/public/js/compiled/out-test-advanced"
                      :main           vimsical.runner
                      :target         :nodejs
                      :optimizations  :advanced
                      :pretty-print   true
                      :pseudo-names   true
                      :parallel-build true}}]}}})
