(ns vimsical.frontend.quick-search.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [re-com.core :as re-com]
   [vimsical.frontend.util.re-frame :refer [<sub <sub-query]]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e->> e>]]
   [vimsical.common.util.core :refer [=by] :as util]
   [vimsical.frontend.quick-search.handlers :as handlers]
   [vimsical.frontend.config :as config]
   [vimsical.frontend.util.search :as util.search]
   [vimsical.frontend.code-editor.handlers :as code-editor.handlers]
   [vimsical.frontend.util.content :as util.content]))

(def commands
  "Map of command keywords -> map of :title and :dispatch vector. Optionally
  takes a :close? value which defaults to true (see dispatch-result)."
  (let [defaults
        {["new" "new vims"] {:title "New Vims"} ;; todo dispatch

         ["play"]           {:title "► Play"} ;; todo dispatch

         ["pause"]          {:title "❚❚ Pause"} ;; todo dispatch

         ["lorem" "ipsum"]  {:title    "Lorem Ipsum"
                             :dispatch [::code-editor.handlers/paste
                                        (util.content/lorem-ipsum 1)]}
         ["go to" "player"] {:title    "Go to Player"
                             :dispatch [::handlers/go-to :route/player]}

         ["go to" "vcr"]    {:title    "Go to VCR"
                             :dispatch [::handlers/go-to :route/vcr]}}
        dev
        {["clear" "console"] {:title    "Clear JS Console"
                              :dispatch [::handlers/clear-console]}}]
    (cond-> defaults
      config/debug? (merge dev))))

(def default-results
  (vec (vals commands)))

(defn search [state query]
  (let [results (or (util.search/search query commands) default-results)]
    (swap! state assoc
           :query query
           :results results)))

(defn move [state dir]
  (let [{:keys [result-idx results]} @state
        max-idx  (dec (count results))
        next-idx (case dir
                   :up (if (zero? result-idx) max-idx (dec result-idx))
                   :down (if (= max-idx result-idx) 0 (inc result-idx)))]
    (swap! state assoc :result-idx next-idx)))

(defn dispatch-result
  ([st]
   (dispatch-result st (:result-idx st)))
  ([st idx]
   (let [{:keys [results]} st
         {:keys [dispatch close?]
          :or   {close? true}} (get results idx)] ; close by default
     (re-frame/dispatch dispatch)
     (when close? (re-frame/dispatch [::handlers/close])))))

(defn handle-key
  [state e]
  (util.dom/handle-key e
                       {:arrow-down #(move state :down)
                        :arrow-up   #(move state :up)
                        :enter      #(dispatch-result @state)
                        :escape     #(re-frame/dispatch [::handlers/close])}))

(defn input [state]
  (reagent/create-class
   {:component-did-mount
    (fn [c]
      (when (not-empty (:query @state))
        (.select (reagent/dom-node c))))
    :render
    (fn [_]
      (let [{:keys [result-idx query results]} @state]
        [:input.input {:id          "IPD"
                       :type        "text"
                       :auto-focus  true
                       :value       query
                       :on-change   (e> (search state value)
                                        (swap! state assoc :result-idx 0))
                       :on-key-down (e->> (handle-key state))
                       :on-blur     (e> (re-frame/dispatch [::handlers/close]))}]))}))

(defn results-view [state]
  (let [{:keys [result-idx results]} @state]
    [:div.search-results
     (for [[idx cmd-map] (map-indexed vector results)
           :let [{:keys [title]} cmd-map
                 is-selected? (= idx result-idx)]]
       [:div.search-result
        {:class         (when is-selected? "selected")
         :on-mouse-down (e>
                         ;; needed to for quick search to close
                         ;; don't ask why
                         (.preventDefault e)
                         (dispatch-result @state idx)
                         ;(transact-cmd this cmd-map)
                         )
         :key           title}
        [:span title]])]))

(defn quick-search []
  (let [state (reagent/atom {:result-idx 0
                             :query      ""
                             :results    default-results})]


    (fn []
      (let [{:quick-search/keys [show?]}
            (<sub-query [:app/quick-search [:db/id
                                            :quick-search/show?]])]
        (let [{:keys [result-idx query results]} @state]
          [:div.quick-search-container
           (when show?
             [:div.quick-search
              [input state]
              (when results
                [results-view state])])])))))