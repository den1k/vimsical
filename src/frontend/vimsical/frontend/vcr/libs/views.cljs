(ns vimsical.frontend.vcr.libs.views
  (:require [vimsical.frontend.vcr.libs.subs :as libs.subs]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.vcs.lib :as vcs.lib]
            [re-com.core :as re-com]
            [clojure.string :as string]
            [net.cgrand.xforms :as x]))

(defn lib-column [[sub-type libs]]
  [:div.results
   [:div.title (string/upper-case (name sub-type))]
   (doall
    (for [{:keys          [added?]
           ::vcs.lib/keys [name version] :as lib} (assoc-in libs [4 :added?] true)]
      [:div.res-row
       [:div.name.cell name]
       [:div.version.cell version]
       [:div.added.cell
        (when added? [re-com/md-icon-button
                      :class "icon"
                      :md-icon-name "zmdi-check"])]
       ]))]
  )

(defn lib-search []
  (let [libs (<sub [::libs.subs/libs-by-sub-type])]
    [re-com/h-box
     :class "lib-search"
     :justify :between
     :gap "30px"
     :children (map lib-column libs)]
    ))

(defn libs []
  [:div.libs
   [lib-search]])