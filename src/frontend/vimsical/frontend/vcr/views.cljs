(ns vimsical.frontend.vcr.views
  (:require
   [re-com.core :refer [v-box h-box box]]
   [vimsical.frontend.views.splits :refer [n-h-split n-v-split]]
   [vimsical.frontend.code-editor.views :refer [code-editor]]
   [reagent.core :as r]
   [vimsical.frontend.util.dom :as util.dom :refer-macros [e-> e-handler]]
   [vimsical.common.util.util :as util]))

(defn resizer [editors-by-type]
  (fn [editors-by-type]
    [:div
     {:style {:flex            "1"
              :display         :flex
              :flex-direction  :column
              :justify-content :space-around
              :align-items     :center}}
     (for [[type {:keys [editor hidden?]}] @editors-by-type]
       [:div
        {:key      type
         :style    (cond-> {:cursor :pointer}
                     hidden? (assoc :color :grey))
         :on-click (e-handler
                    (swap! editors-by-type
                           update-in [type :hidden?]
                           not))}
        (name type)])]))

(def file-types
  [:html :css :javascript])

(defn wrap-editor [file-type]
  {:file-type file-type
   :editor    ^{:key file-type} [code-editor file-type]
   :hidden?   false})

(defn visible-editors [editors-by-type]
  (->> editors-by-type
       vals
       (remove :hidden?)
       (mapv :editor)))

(defn editors-by-type [file-types]
  (->> file-types
       (map wrap-editor)
       (util/project :file-type)))

(defn vcr []
  (let [editors-by-type (r/atom (editors-by-type file-types))]
    (fn []
      (let [visible-editors (visible-editors @editors-by-type)]
        [v-box
         :class "vcr"
         ;; Styles to hide code editors' (Monaco) overflow
         :style {:display  "relative"
                 :overflow "hidden"}
         :size "100%"
         :children
         [[:div "timeline"]
          [n-h-split
           :panels [[:h1 "live-preview"]
                    [n-v-split
                     :height "100%"
                     :splitter-size "30px"
                     :panels visible-editors
                     :margin "0"]]
           :splitter-child [resizer editors-by-type]
           :splitter-size "100px"
           :initial-split 60
           :margin "0"]]]))))
