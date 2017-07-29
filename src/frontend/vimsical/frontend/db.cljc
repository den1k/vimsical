(ns vimsical.frontend.db
  (:refer-clojure :exclude [uuid])
  (:require
   [vimsical.subgraph :as sg]
   [re-frame.core :as re-frame]
   [vimsical.common.test :refer [uuid]]
   [vimsical.frontend.quick-search.commands :as quick-search.commands]
   [vimsical.frontend.util.subgraph :as util.sg]
   [vimsical.vcs.compiler :as compiler]
   [vimsical.vcs.lib :as lib]
   [vimsical.frontend.router.routes :as router.routes]))

(def libs
  [
   ;; JS
   (lib/new-lib "Angular" "1.6.1" :javascript "https://cdnjs.cloudflare.com/ajax/libs/angular.js/1.6.1/angular.min.js")
   (lib/new-lib "Backbone" "1.3.3" :javascript "https://cdnjs.cloudflare.com/ajax/libs/backbone.js/1.3.3/backbone-min.js")
   (lib/new-lib "Bootstrap" "3.3.7" :javascript "https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.3.7/js/bootstrap.min.js")
   (lib/new-lib "Bootstrap" "4.0.0" :javascript "https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.0.0-alpha.6/js/bootstrap.min.js")
   (lib/new-lib "D3" "4.5.0" :javascript "https://cdnjs.cloudflare.com/ajax/libs/d3/4.5.0/d3.min.js")
   (lib/new-lib "Ember" "2.11.0" :javascript "https://cdnjs.cloudflare.com/ajax/libs/ember.js/2.11.0/ember.min.js")
   (lib/new-lib "TweenMax" "1.19.1" :javascript "https://cdnjs.cloudflare.com/ajax/libs/gsap/1.19.1/TweenMax.min.js")
   (lib/new-lib "Handlebars" "4.0.6" :javascript "https://cdnjs.cloudflare.com/ajax/libs/handlebars.js/4.0.6/handlebars.min.js")
   (lib/new-lib "jQuery" "3.1.1" :javascript "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.1.1/jquery.min.js")
   (lib/new-lib "jQuery UI" "1.12.1" :javascript "https://cdnjs.cloudflare.com/ajax/libs/jqueryui/1.12.1/jquery-ui.min.js")
   (lib/new-lib "Lodash" "4.17.4" :javascript "https://cdnjs.cloudflare.com/ajax/libs/lodash.js/4.17.4/lodash.min.js")
   (lib/new-lib "Modernizr" "2.8.3" :javascript "https://cdnjs.cloudflare.com/ajax/libs/modernizr/2.8.3/modernizr.min.js")
   (lib/new-lib "P5" "0.5.11" :javascript "https://cdnjs.cloudflare.com/ajax/libs/p5.js/0.5.11/p5.min.js")
   (lib/new-lib "Polyfill.io" "2" :javascript "https://cdn.polyfill.io/v2/polyfill.min.js")
   (lib/new-lib "Polymer" "0.5.6" :javascript "https://cdnjs.cloudflare.com/ajax/libs/polymer/0.5.6/polymer.min.js")
   (lib/new-lib "React" "15.4.2" :javascript "https://cdnjs.cloudflare.com/ajax/libs/react/15.4.2/react.min.js")
   (lib/new-lib "React Dom" "15.4.2" :javascript "https://cdnjs.cloudflare.com/ajax/libs/react/15.4.2/react-dom.min.js")
   (lib/new-lib "Snap.svg" "0.4.1" :javascript "https://cdnjs.cloudflare.com/ajax/libs/snap.svg/0.4.1/snap.svg-min.js")
   (lib/new-lib "Three.js" "r84" :javascript "https://cdnjs.cloudflare.com/ajax/libs/three.js/84/three.min.js") ; versioned by release number
   (lib/new-lib "Underscore" "1.8.3" :javascript "https://cdnjs.cloudflare.com/ajax/libs/underscore.js/1.8.3/underscore-min.js")
   (lib/new-lib "Vue" "2.1.10" :javascript "https://cdnjs.cloudflare.com/ajax/libs/vue/2.1.10/vue.min.js")
   (lib/new-lib "Zepto" "1.2.0" :javascript "https://cdnjs.cloudflare.com/ajax/libs/zepto/1.2.0/zepto.min.js")
   (lib/new-lib "Zingchart" "latest" :javascript "https://cdn.zingchart.com/zingchart.min.js")
   ;; CSS
   (lib/new-lib "Bootstrap" "4.0.0-alpha.6" :css "https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/4.0.0-alpha.6/css/bootstrap.min.css")
   (lib/new-lib "Bootstrap" "3.3.7" :css "https://cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.3.7/css/bootstrap.min.css")
   (lib/new-lib "Foundation" "6.3.0" :css "https://cdnjs.cloudflare.com/ajax/libs/foundation/6.3.0/css/foundation.min.css")
   (lib/new-lib "Animate.css" "3.5.2" :css "https://cdnjs.cloudflare.com/ajax/libs/animate.css/3.5.2/animate.min.css")
   (lib/new-lib "Materialize" "0.98.2" :css "https://cdnjs.cloudflare.com/ajax/libs/materialize/0.98.2/css/materialize.min.css")
   (lib/new-lib "Bulma" "0.4.2" :css "https://cdnjs.cloudflare.com/ajax/libs/bulma/0.4.2/css/bulma.min.css")])

(def compilers
  [{:db/uid                (uuid :babel-compiler)
    ::compiler/name        "Babel"
    ::compiler/type        :text
    ::compiler/sub-type    :babel
    ::compiler/to-sub-type :javascript}])

(def state
  {:app/user         {:db/uid (uuid)}
   :app/vims         nil
   :app/quick-search {:db/uid                           (uuid :quick-search)
                      :quick-search/show?               false
                      :quick-search/result-idx          0
                      :quick-search/query               ""
                      :quick-search/commands            quick-search.commands/commands
                      :quick-search/filter-idx          nil
                      :quick-search/filter-result-idx   nil
                      :quick-search/filter-category-idx nil}
   :app/libs         libs
   :app/compilers    compilers
   :app/modal        nil})

;;
;; * Mapgraph db
;;

(defn new-db
  [state]
  (-> (sg/new-db)
      (sg/add-id-attr :db/uid ::lib/src)
      (util.sg/add-linked-entities state)))

(def default-db (new-db state))

;;
;; * Re-frame
;;

(re-frame/reg-event-db ::init (constantly default-db))
(re-frame/reg-sub ::db (fn [db _] db))
