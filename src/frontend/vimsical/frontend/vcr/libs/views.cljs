(ns vimsical.frontend.vcr.libs.views
  (:require [vimsical.frontend.vcr.libs.subs :as libs.subs]
            [re-frame.core :as re-frame]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.frontend.util.dom :refer-macros [e>]]
            [vimsical.vcs.lib :as vcs.lib]
            [vimsical.frontend.vcr.libs.handlers :as handlers]
            [re-com.core :as re-com]
            [clojure.string :as string]))

(defn lib-column [[sub-type libs]]
  [:div.results
   [:div.title (string/upper-case (name sub-type))]
   (doall
    (for [{:keys          [added?]
           ::vcs.lib/keys [name version src] :as lib} libs]
      [:div.res-row
       {:key      src
        :on-click (e> (.stopPropagation e)
                      (re-frame/dispatch [::handlers/toggle-lib lib]))}
       [:div.name.cell name]
       [:div.version.cell version]
       [:div.added.cell
        (when added? [re-com/md-icon-button
                      :class "icon"
                      :md-icon-name "zmdi-check"])]]))])

(defn lib-search []
  (let [libs (<sub [::libs.subs/libs-by-sub-type])]
    [re-com/h-box
     :class "lib-search"
     :justify :between
     :gap "30px"
     :children (map lib-column libs)]))

(defn libs []
  [:div.libs
   [lib-search]])