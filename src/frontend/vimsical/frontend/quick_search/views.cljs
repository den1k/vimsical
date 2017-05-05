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
   [vimsical.vcs.compiler :as compiler]
   [clojure.string :as string]))

(defn handle-key
  [e {:keys [quick-search] :as opts}]
  (util.dom/handle-key
   e
   {:arrow-down  #(re-frame/dispatch [::handlers/move :down opts])
    :arrow-up    #(re-frame/dispatch [::handlers/move :up opts])
    :arrow-left  #(re-frame/dispatch [::handlers/move :left opts])
    :arrow-right #(re-frame/dispatch [::handlers/move :right opts])
    :tab         #(re-frame/dispatch [::handlers/move :right opts])
    :enter       #(re-frame/dispatch [::handlers/run-selected-cmd quick-search])
    :escape      #(re-frame/dispatch [::handlers/close])}))

(defn input []
  (reagent/create-class
   {:component-did-mount
    (fn [c]
      (when (not-empty (:quick-search/query (<sub [::subs/quick-search [:quick-search/query]])))
        (.select (reagent/dom-node c))))
    :render
    (fn [_]
      (let [{:quick-search/keys [query] :as qs} (<sub [::subs/quick-search])
            results (<sub [::subs/results])
            filters (<sub [::subs/filters])]
        [:input.input {:type        "text"
                       :auto-focus  true
                       :value       query
                       :on-change   (e>
                                     (re-frame/dispatch [::handlers/set-query value qs]))
                       :on-key-down (e-> (handle-key
                                          {:quick-search qs
                                           :results      results
                                           :filters      filters}))
                       :on-blur     (e> (re-frame/dispatch [::handlers/close]))}]))}))

(defn results-view []
  (fn []
    (if-let [filter-cmds (<sub [::subs/selected-filter-results])] ;; should be results
      [re-com/h-box
       :class "filter-results ac"
       :justify :around
       :children
       (for [[category cmds] filter-cmds
             :let [title (string/upper-case (name category))]]
         [:div.category-box
          {:key category}
          [:div.title.jc title]
          [:div.search-results
           (for [{:keys [title dispatch selected?]} cmds]
             [:div.search-result
              {:key   title
               :class (when selected? "selected")}
              title])]])]
      (let [results (<sub [::subs/results])]
        [:div.search-results
         (for [{:keys [title selected?] :as res} results]
           [:div.search-result
            {:class         (when selected? "selected")
             :on-mouse-down (e>
                             ;; needed to for quick search to close
                             ;; don't ask why
                             (.preventDefault e)
                             (re-frame/dispatch [::handlers/run-cmd res]))
             :on-mouse-move (e> (re-frame/dispatch [::handlers/update-result-idx
                                                    (<sub [::subs/quick-search])
                                                    results res]))
             :key           title}
            [:span title]])]))))

(defn quick-search []
  (fn []
    (let [{:quick-search/keys [show?
                               commands
                               query
                               result-idx
                               filter] :as qs}
          (<sub-query [:app/quick-search ['*]])]
      [:div.quick-search-container
       (when show?
         (let [filters (<sub [::subs/filters])]
           [:div.quick-search
            [re-com/h-box
             :class "input-and-filters"
             :gap "10px"
             :children [[input]
                        [re-com/h-box
                         :class "filters"
                         :gap "10px"
                         :align :center
                         :children (for [[{:keys [title selected?]} cmds] filters]
                                     [:div.title-bubble
                                      {:class (when selected? "selected")}
                                      title])]]]
            [results-view]]))])))