(defproject vimsical "0.1.0-SNAPSHOT"
  :dependencies
  [[org.clojure/clojure "1.9.0-alpha14"]]

  :source-paths []                      ; ignore src/ in all profiles

  :clean-targets
  ^{:protect false}
  ["resources/public/js/compiled" "target"]

  :profiles
  {:dev
   {:plugins
    [[lein-pprint  "1.1.2"]             ; lein with-profile frontend-dev pprint
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
   {:source-paths ["src/common"]
    :dependencies
    [[com.cognitect/transit-cljs "0.8.239"]
     [org.clojure/core.async     "0.2.395"]
     [com.stuartsierra/component "0.3.1"]
     [environ                    "1.1.0"]]}

   ;;
   ;; Backend
   ;;
   :backend
   [:vcs :common
    {:source-paths ["src/backend"]
     :repositories
     {"my.datomic.com"
      {:url      "https://my.datomic.com/repo"
       :username :env/datomic_login
       :password :env/datomic_password}}
     :dependencies
     [[com.taoensso/carmine    "2.15.0"]
      [org.immutant/web        "2.1.5" :exclusions [ring/ring-core]]
      [cc.qbits/alia-all       "3.3.0"]
      [com.datomic/datomic-pro "0.9.5544" :exclusions [commons-codec]]]
     :main         vimsical.backend.core}]

   :backend-dev
   [:backend
    {:dependencies
     [[criterium "0.4.4"]]}]

   ;;
   ;; Frontend
   ;;
   :frontend
   [:vcs :common :cljs
    {:source-paths ["src/frontend"]
     :plugins
     [[lein-cljsbuild "1.1.4"
       :exclusions [org.apache.commons/commons-compress]]
      [lein-garden   "0.2.8"
       :exclusions [org.clojure/clojure]]]
     :dependencies
     [[org.clojure/clojurescript      "1.9.293"]
      [cljsjs/codemirror              "5.11.0-2"]
      [garden                         "1.3.2"]
      ;; Added this to fix a compilation issue with garden
      [ns-tracker                     "0.3.0"]
      [cljsjs/google-diff-match-patch "20121119-1"]
      [com.stuartsierra/mapgraph      "0.2.1"]
      [reagent                        "0.6.1"]
      [re-frame                       "0.9.2"]]
     :garden
     {:builds
      [{:id           "dev-styles"
        :source-paths ["stylesheets"]
        :stylesheet   vimsical.frontend/styles
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
      [figwheel-sidecar        "0.5.9"]
      [binaryage/devtools      "0.8.3"]]
     :repl-options
     {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
     :figwheel
     {:css-dirs ["resources/public/css"]}}]

   ;;
   ;; cljs
   ;;
   :cljs
   {:cljsbuild
    {:builds
     [{:id           "prod"
       :jar          true
       :source-paths ["src/frontend" "src/common" "src/vcs"]
       :compiler     {:main           vimsical.frontend.core
                      :asset-path     "/js"
                      :externs        ["resources/externs/svg.js"]
                      :output-to      "resources/public/js/compiled/vimsical.js"
                      :optimizations  :advanced
                      :pretty-print   false
                      :parallel-build true
                      ;; Determines whether readable names are emitted. This can
                      ;; be useful when debugging issues in the optimized
                      ;; JavaScript and can aid in finding missing
                      ;; externs. Defaults to false.
                      :pseudo-names   false}}
      {:id           "dev"
       :figwheel     {:on-jsload vimsical.frontend.core/-main}
       :source-paths ["src/frontend" "src/common" "src/vcs" "dev/frontend"]
       :compiler     {:main            vimsical.frontend.core
                      :asset-path      "/js/compiled/out"
                      :output-to       "resources/public/js/compiled/vimsical.js"
                      :output-dir      "resources/public/js/compiled/out"
                      :optimizations   :none
                      :parallel-build  true
                      :preloads        [devtools.preload]
                      :external-config {:devtools/config
                                        {:features-to-install [:formatters :hints]
                                         :fn-symbol           "F"}}}}]}}})
