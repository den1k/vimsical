(ns vimsical.frontend.share.views
  (:require [vimsical.frontend.player.views.player :refer [player]]
            [vimsical.frontend.player.embed :as player.embed]
            [vimsical.frontend.util.dom :refer-macros [e>]]
            [re-frame.interop :as interop]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [re-com.core :as re-com]
            [vimsical.frontend.app.subs :as app.subs]))

(defn embed-preview []
  (let [vims (<sub [::app.subs/vims [:db/uid]])]
    [:div.embed-preview
     [player {:orientation :landscape
              :vims        vims}]]))

(defn share []
  (let [copied? (interop/ratom false)]
    (fn []
      (let [vims         (<sub [::app.subs/vims])
            embed-markup (player.embed/player-iframe-markup vims)]
        [:div.share.dc.ac
         [:div.share-options
          {:on-click (e> (.stopPropagation e))}
          [:div.embed.dc
           [:h1 "Embed"]
           [embed-preview]
           [:div.markup-and-copy.f1
            [:pre.embed-markup
             {:id "embed-markup"}
             embed-markup]
            [:div.button.copy-to-clipboard
             {:data-clipboard-text embed-markup
              :on-click            (e> (reset! copied? true))
              :on-mouse-leave      (e> (reset! copied? false))}
             (if @copied? "Copied!" "Copy")]]]]]))))
