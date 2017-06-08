(ns vimsical.frontend.share.views
  (:require [vimsical.frontend.player.views.player :refer [player]]
            [vimsical.frontend.player.embed :as player.embed]
            [vimsical.frontend.util.dom :refer-macros [e>]]
            [re-frame.interop :as interop]
            [re-com.core :as re-com]))

(defn embed-preview [{:keys [vims]}]
  [:div.embed-preview
   [player {:vims vims}]])


(defn share []
  (let [copied? (interop/ratom false)]
    (fn [{:keys [vims]}]
      (let [embed-markup (player.embed/player-iframe-markup
                          {:src   "http://localhost:3449/player.html"
                           :style {:border :none
                                   :width  "100%"
                                   :height 400}})]
        [:div.share.dc.ac
         [:div.share-options
          {:on-click (e> (.stopPropagation e))}
          [:div.embed.dc
           [:h1 "Embed"]
           [embed-preview {:vims vims}]
           [:div.markup-and-copy.f1
            [:pre.embed-markup
             {:id "embed-markup"}
             embed-markup]
            [:div.button.copy-to-clipboard
             {:data-clipboard-text embed-markup
              :on-click            (e> (reset! copied? true))
              :on-mouse-leave      (e> (reset! copied? false))}
             (if @copied? "Copied!" "Copy")]]]]]))))
