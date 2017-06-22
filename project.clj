(defproject vimsical "0.1.0-SNAPSHOT"
  :dependencies
  [[org.clojure/clojure "1.9.0-alpha17"]
   [org.clojure/spec.alpha "0.1.123"]
   [org.clojure/test.check "0.9.0"]]

  :source-paths []                      ; ignore src/ in all profiles
  :test-paths []

  :clean-targets
  ^{:protect false}
  ["resources/public/js/compiled/" "target/"]

  :profiles
  {:dev
   [{:dependencies
     ;; Help CIDER find the Java code in Clojure
     [[org.clojure/clojure "1.9.0-alpha17" :classifier "sources"]]
     :plugins
     [[lein-pprint "1.1.2"]             ; lein with-profile frontend-dev pprint
      [lein-environ "1.1.0"]]}]

   :test
   {:dependencies
    [[orchestra "0.3.0"]]
    :global-vars
    {*assert* true *warn-on-reflection* false *unchecked-math* false}}

   :uberjar
   [:backend-log-prod
    {:aot          [vimsical.backend.core]
     :omit-source  true
     :uberjar-name "vimsical.jar"
     :main         vimsical.backend.core}]

   :bench
   [:common :vcs :dev
    {:main         vimsical.vcs.bench
     :source-paths ["bench/vcs"]
     :dependencies [[criterium "0.4.4"]]}]

   ;;
   ;; Vcs
   ;;
   :vcs
   {:source-paths ["src/vcs"]
    :dependencies [[org.clojure/data.avl "0.0.17"]
                   [diffit "1.0.0" :exclusions [org.clojure/tools.reader]]
                   [net.cgrand/xforms "0.9.3" :exclusions [org.clojure/clojurescript]]]}
   ;;
   ;; Common
   ;;
   :common
   {:source-paths ["src/common"]
    :dependencies [[com.cognitect/transit-clj "0.8.300"]
                   [com.cognitect/transit-cljs "0.8.239"]
                   [org.clojure/core.async "0.3.443"]
                   [com.stuartsierra/component "0.3.2"]
                   [medley "1.0.0"]
                   [environ "1.1.0"]
                   [com.lucasbradstreet/cljs-uuid-utils "1.0.2"]]}
   ;;
   ;; Backend
   ;;
   :backend
   [{:source-paths   ["src/backend"]
     :resource-paths ["resources" "resources/backend" "resources/backend/logback/prod"]
     :main           vimsical.backend.core
     :repositories   {"my.datomic.com"
                      {:url      "https://my.datomic.com/repo"
                       :username :env/datomic_login
                       :password :env/datomic_password}}
     :dependencies   [[com.taoensso/carmine "2.16.0" :exclusions [org.clojure/tools.reader]]
                      [cc.qbits/alia "3.2.0"]
                      [cc.qbits/alia-async "3.2.0"]
                      [cc.qbits/alia-nippy "3.1.4"]
                      [cc.qbits/hayt "4.0.0"]
                      [com.datomic/datomic-pro "0.9.5544" :exclusions [commons-codec org.slf4j/slf4j-nop org.slf4j/slf4j-log4j12]]
                      [com.amazonaws/aws-java-sdk-dynamodb "1.11.6"
                       :exclusions [joda-time commons-logging]]
                      [ch.qos.logback/logback-classic "1.2.3"]
                      [org.clojure/tools.logging "0.3.1"]
                      ;; HTTP stack
                      [io.pedestal/pedestal.service "0.5.2"]
                      [io.pedestal/pedestal.immutant "0.5.2" :exclusions [org.jboss.logging/jboss-logging ch.qos.logback/logback-classic]]
                      [buddy/buddy-hashers "1.2.0"]]
     :global-vars    {*assert* true *warn-on-reflection* true *unchecked-math* :warn-on-boxed}}
    :vcs :common]

   :backend-log-prod
   {:resource-paths ["resources/backend/logback/prod"]}

   :backend-log-dev
   {:resource-paths ["resources/backend/logback/dev"]}

   :backend-log-test
   {:resource-paths ["resources/backend/logback/test"]}

   :backend-dev
   [:backend
    :env.backend/dev
    :backend-log-dev
    {:dependencies
     [[criterium "0.4.4"]]
     ;; Get proper deps resolution for fixtures etc
     :source-paths ["dev/backend" "test/vcs" "test/backend" "test/common"]}]

   :backend-test
   [:backend
    :test
    :env.backend/dev
    :env.backend/test
    :backend-log-test
    :vcs :common
    {:test-paths ["test/backend" "test/vcs" "test/common"]}]
   ;;
   ;; Frontend
   ;;
   :-frontend-config
   {:source-paths ["src/frontend"]
    :plugins      [[lein-cljsbuild "1.1.6"
                    :exclusions [org.apache.commons/commons-compress]]]
    :repositories {"jitpack" {:url "https://jitpack.io"}}
    :dependencies [[org.clojure/clojurescript "1.9.562" :exclusions [org.clojure/tools.reader]]
                   ;; Our mapgraph fork. Must be be symlinked in checkouts.
                   [com.github.vimsical/mapgraph "parser-SNAPSHOT" :exclusions [org.clojure/clojure re-frame]]
                   [reagent "0.6.2" :exclusions [org.clojure/clojurescript]]
                   [re-frame "0.9.4" :exclusions [org.clojure/clojurescript]]
                   [com.andrewmcveigh/cljs-time "0.5.0"] ; required re-com, but we need a newer version
                   [re-com "2.1.0" :exclusions [reagent org.clojure/clojurescript org.clojure/core.async com.andrewmcveigh/cljs-time]]
                   [com.github.vimsical/re-frame-async-flow-fx "-SNAPSHOT" :exclusions [re-frame org.clojure/clojurescript]]
                   [thi.ng/color "1.2.0"]
                   [cljsjs/clipboard "1.6.1-1"]
                   [bidi "2.1.1"]
                   [kibu/pushy "0.3.7"]]}

   :-frontend-dev-config
   {:source-paths ["dev/frontend"]
    :plugins      [[lein-figwheel "0.5.9" :exclusions [[org.clojure/clojure]]]]
    :dependencies [[com.cemerick/piggieback "0.2.2-SNAPSHOT"]
                   [figwheel-sidecar "0.5.10" :exclusions [org.clojure/clojurescript]]
                   [re-frisk "0.4.5" :exclusions [re-frame org.clojure/clojurescript]]
                   ;; needed as a dep for re-frame.trace
                   [binaryage/devtools "0.9.4"]
                   ;; re-frame.trace - clone and install to use
                   ;; https://github.com/Day8/re-frame-trace
                   [day8.re-frame/abra "0.0.9" :exclusions [re-frame reagent org.clojure/clojurescript]]
                   [org.clojure/tools.nrepl "0.2.13"]
                   ;; Custom ring handler for figwheel, match pedestal dependecy
                   ;; vector to avoid conflicts in the integration target
                   [ring/ring-core "1.5.1" :exclusions [org.clojure/clojure org.clojure/tools.reader crypto-random crypto-equality]]]
    :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}

   :frontend
   [:vcs :common :cljs :css :-frontend-config]

   :frontend-dev
   [:frontend :-frontend-dev-config :env.frontend/dev]

   :frontend-test
   [:test :frontend :env.frontend/dev :vcs :common
    {:test-paths ["test/frontend" "test/vcs" "test/common"]
     :plugins    [[lein-doo "0.1.7"]]}]

   :player
   [:vcs :common :css-player :-frontend-config]

   :player-dev
   [:player :-frontend-dev-config :env.frontend/dev]

   :integration-dev
   [{:source-paths ["test/integration"]
     :test-paths   ^{:protect false} ["test/integration"]
     :dependencies [[day8.re-frame/test "0.1.5" :exclusions [re-frame org.clojure/clojurescript]]
                    ;; Need more exclusions because guava conflicts with datomic
                    [org.clojure/clojurescript "1.9.562" :exclusions [com.google.guava/guava org.clojure/tools.reader]]]}
    :-frontend-config
    :env.frontend/dev
    :backend-test
    :backend-log-dev]

   :integration-test
   [:integration-dev :backend-log-test]

   ;;
   ;; CSS
   ;;
   ;; NOTE these profiles won't run by themselves since they need some frontend deps
   ;; Example: `lein with-profile frontend-dev garden auto`

   :-css-dev-config

   {:plugins      [[lein-garden "0.3.0" :exclusions [org.clojure/clojure]]]
    ;;:prep-tasks   [["garden" "once"]]
    :dependencies [[garden "1.3.2"]
                   ;; Added this to fix a compilation issue with garden
                   [ns-tracker "0.3.1"]]}

   :css
   [:-css-dev-config
    {:garden
     {:builds
      [{:id           "dev-styles"
        :source-paths ["src/frontend" "src/common"]
        :stylesheet   vimsical.frontend.styles.core/styles
        :compiler     {:output-to     "resources/public/css/app.css"
                       :vendors       ["webkit" "moz"]
                       :auto-prefix   #{:user-select}
                       :pretty-print? true}}]}}]

   :css-player
   [:-css-dev-config
    {:garden
     {:builds
      [{:id           "dev-styles-player"
        :source-paths ["src/frontend" "src/common"]
        :stylesheet   vimsical.frontend.player.style/embed-styles
        :compiler     {:output-to     "resources/public/css/player.css"
                       :vendors       ["webkit" "moz"]
                       :auto-prefix   #{:user-select}
                       :pretty-print? true}}]}}]

   ;;
   ;; Cljs
   ;;
   :cljs
   {:cljsbuild
    {:builds
     [{:id           "prod"
       :jar          true
       :source-paths [ ;; "checkouts/mapgraph/src"
                      ;; "checkouts/re-frame-async-flow-fx/src"
                      ;; "checkouts/re-frame-forward-events-fx/src"
                      "src/frontend"
                      "src/common"
                      "src/vcs"]
       :compiler     {:main          vimsical.frontend.core
                      :asset-path    "/js"
                      :externs       ["resources/externs/svg.js"]
                      :output-to     "resources/public/js/compiled/vimsical.js"
                      :output-dir    "resources/public/js/compiled/out-prod"
                      :optimizations :advanced
                      :infer-externs true

                      :parallel-build  true
                      :closure-defines {goog.DEBUG false}

                      ;; debug
                      ;; Determines whether readable names are emitted. This can
                      ;; be useful when debugging issues in the optimized
                      ;; JavaScript and can aid in finding missing
                      ;; externs. Defaults to false.
                      :pseudo-names false
                      :pretty-print false
                      :verbose      false}}
      {:id           "dev"
       :figwheel     {:on-jsload vimsical.frontend.dev/on-reload}
       :source-paths [ ;; "checkouts/mapgraph/src"
                      ;; "checkouts/re-frame-async-flow-fx/src"
                      ;; "checkouts/re-frame-forward-events-fx/src"
                      "dev/frontend"
                      "src/frontend"
                      "src/common"
                      "src/vcs"
                      "dev/frontend"]
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
      {:id           "player-dev"
       :figwheel     {:on-jsload vimsical.frontend.player.dev/on-reload}
       :source-paths [ ;; "checkouts/mapgraph/src"
                      ;; "checkouts/re-frame-async-flow-fx/src"
                      ;; "checkouts/re-frame-forward-events-fx/src"
                      "dev/frontend"
                      "src/frontend"
                      "src/common"
                      "src/vcs"
                      "dev/frontend"]
       :compiler     {:main                 vimsical.frontend.player.core
                      :asset-path           "/js/compiled/player/out"
                      :output-to            "resources/public/js/compiled/vimsical-player.js"
                      :output-dir           "resources/public/js/compiled/player/out"
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
      {:id           "player-prod"
       :jar          true
       :source-paths ["checkouts/mapgraph/src" "src/frontend" "src/common" "src/vcs"]
       :compiler     {:main          vimsical.frontend.player.core
                      :asset-path    "/js"
                      :externs       ["resources/externs/svg.js"]
                      :output-to     "resources/public/js/compiled/vimsical-player.js"
                      :output-dir    "resources/public/js/compiled/player/out-prod"
                      :optimizations :advanced
                      :infer-externs true

                      :parallel-build  true
                      :closure-defines {goog.DEBUG false}

                      ;; debug
                      ;; Determines whether readable names are emitted. This can
                      ;; be useful when debugging issues in the optimized
                      ;; JavaScript and can aid in finding missing
                      ;; externs. Defaults to false.
                      :pseudo-names false
                      :pretty-print false
                      :verbose      false}}
      {:id           "test"
       :source-paths [ ;; "checkouts/mapgraph/src"
                      ;; "checkouts/re-frame-async-flow-fx/src"
                      ;; "checkouts/re-frame-forward-events-fx/src"
                      "src/frontend"
                      "src/common"
                      "src/vcs"
                      "test/frontend"
                      "test/common"
                      "test/vcs"
                      "test/runner"]
       :compiler     {:output-to      "resources/public/js/compiled/vimsical-test.js"
                      :output-dir     "resources/public/js/compiled/out-test"
                      :main           vimsical.runner
                      :target         :nodejs
                      :optimizations  :none
                      :parallel-build true}}
      {:id           "test-advanced"
       :source-paths [ ;; "checkouts/mapgraph/src"
                      ;; "checkouts/re-frame-async-flow-fx/src"
                      ;; "checkouts/re-frame-forward-events-fx/src"
                      "src/frontend"
                      "src/common"
                      "src/vcs"
                      "test/frontend"
                      "test/common"
                      "test/vcs"
                      "test/runner"]
       :compiler     {:output-to      "resources/public/js/compiled/vimsical-test.js"
                      :output-dir     "resources/public/js/compiled/out-test-advanced"
                      :main           vimsical.runner
                      :target         :nodejs
                      :optimizations  :advanced
                      :pretty-print   true
                      :pseudo-names   true
                      :parallel-build true}}]}}})
