(ns vimsical.frontend.vcr.libs.views
  (:require [vimsical.frontend.vcr.libs.subs :as subs]
            [re-frame.core :as re-frame]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.frontend.util.dom :refer-macros [e>]]
            [vimsical.vcs.lib :as vcs.lib]
            [vimsical.frontend.vcr.libs.handlers :as handlers]
            [re-com.core :as re-com]
            [clojure.string :as string]
            [re-frame.interop :as interop]))

(defn infer-sub-type [src-str]
  (some->> (string/split src-str ".")
           last
           (get {"js" :javascript "css" :css})))

(defn custom []
  (let [lib-src  (interop/ratom nil)
        sub-type (interop/ratom false)]
    (fn []
      (let [added-anon-libs (<sub [::subs/added-anon-libs])]
        [:div.custom
         {:on-click (e> (.stopPropagation e))}
         [:h1 "Custom Add"]
         [:div.add.jsb
          [:input.src-input
           {:placeholder "https://my-custom-lib.com"
            :value       @lib-src
            :on-change   (e> (reset! sub-type (infer-sub-type value))
                             (reset! lib-src value))}]
          [re-com/gap :size "20px"]
          [:div.add-button.button
           (if (some? @sub-type)
             {:on-click
              (e> (when-some [sub-type @sub-type]
                    (re-frame/dispatch
                     [::handlers/add-lib (vcs.lib/new-lib sub-type @lib-src)])
                    (reset! lib-src nil)))}
             {:class "invalid"})
           (if (some? @sub-type) "Add lib" "Invalid Extension")]]
         [:div.added
          (doall
           (for [{::vcs.lib/keys [src] :as lib} added-anon-libs]
             [:div.added-lib.jsb.ac
              {:on-click (e> (re-frame/dispatch [::handlers/remove-lib lib]))}
              src
              [re-com/md-icon-button
               :class "close-icon"
               :size :smaller
               :md-icon-name "zmdi-close"]]))]]))))

(defn lib-column [[sub-type libs]]
  [:div.column
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

(defn catalogue []
  (let [libs (<sub [::subs/libs-by-sub-type])]
    [:div.catalogue
     [:h1 "Catalogue"]
     [re-com/h-box
      :class "columns"
      :justify :between
      :gap "30px"
      :children (map lib-column libs)]]))

(defn libs []
  [:div.libs
   [custom]
   [catalogue]])