(ns vimsical.frontend.quick-search.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [re-com.core :as re-com]
   [vimsical.frontend.util.re-frame :refer [<sub <sub-query]]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e->> e>]]
   [vimsical.common.util.core :refer [=by] :as util]
   [vimsical.frontend.quick-search.subs :as subs]
   [vimsical.frontend.quick-search.handlers :as handlers]
   [vimsical.frontend.util.search :as util.search]
   [vimsical.vcs.lib :as lib]
   [vimsical.vcs.compiler :as compiler]))

(defn handle-key
  [quick-search results e]
  (util.dom/handle-key
   e
   {:arrow-down #(re-frame/dispatch [::handlers/move quick-search results :down])
    :arrow-up   #(re-frame/dispatch [::handlers/move quick-search results :up])
    :enter      #(re-frame/dispatch [::handlers/run-selected-cmd quick-search])
    :escape     #(re-frame/dispatch [::handlers/close])}))

(defn input []
  (reagent/create-class
   {:component-did-mount
    (fn [c]
      (when (not-empty (:quick-search/query (<sub [::subs/quick-search [:quick-search/query]])))
        (.select (reagent/dom-node c))))
    :render
    (fn [_]
      (let [{:quick-search/keys [query] :as qs} (<sub [::subs/quick-search])
            results (<sub [::subs/results])]
        [:input.input {:id          "IPD"
                       :type        "text"
                       :auto-focus  true
                       :value       query
                       :on-change   (e>
                                     (re-frame/dispatch [::handlers/set-query value qs]))
                       :on-key-down (e->> (handle-key qs results))
                       :on-blur     (e> (re-frame/dispatch [::handlers/close]))}]))}))

(defn results-view []
  (let [hovering? (reagent/atom false)]
    (fn []
      [:div.search-results
       {:on-mouse-enter (e> (reset! hovering? true))
        :on-mouse-leave (e> (reset! hovering? false))}
       (doall
        (for [{:keys [title selected?] :as res} (<sub [::subs/results])]
          [:div.search-result
           {:class         (when (and selected? (not @hovering?)) "selected")
            :on-mouse-down (e>
                            ;; needed to for quick search to close
                            ;; don't ask why
                            (.preventDefault e)
                            (re-frame/dispatch [::handlers/run-cmd res])
                            )
            :key           title}
           [:span title]]))])))

(defn quick-search []
  (let [{:quick-search/keys [show?
                             commands
                             query
                             result-idx
                             filter] :as qs}
        (<sub-query [:app/quick-search ['*]])]
    [:div.quick-search-container
     (when show?
       [:div.quick-search
        [re-com/h-box
         :class "input-and-filters"
         :gap "10px"
         :children [[input]
                    #_[re-com/h-box
                       :class "filters"
                       :gap "10px"
                       :align :center
                       :children (for [[type filters] filters]
                                   [:div.title-bubble (name type)])]]]
        [results-view]])]))