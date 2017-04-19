(ns vimsical.frontend.player.views
  (:require [re-com.core :as re-com]
            [vimsical.frontend.live-preview.views :refer [live-preview]]
            [vimsical.frontend.code-editor.views :refer [code-editor]]
            [vimsical.frontend.views.splits :as splits]
            [vimsical.frontend.views.shapes :as shapes]
            [vimsical.frontend.views.user :as user]
            [vimsical.frontend.util.dom :refer-macros [e> e->]]
            [reagent.core :as reagent]
            [vimsical.frontend.views.icons :as icons]
            [vimsical.frontend.util.dom :as util.dom]))

(defn play-symbol [opts]
  [shapes/triangle
   (merge {:class           "play-symbol"
           :origin          [50 50]
           :height          50
           :stroke-linejoin "round"
           :stroke-width    8
           :rotate          90}
          opts)])

;; share with vcr
(defn pause-symbol
  "Pause Symbol to be used within SVG. Uses rect instead of line to for border-
  radius control."
  [{:keys [origin bar-width gap-width height border-radius class]
    :or   {origin        [50 50]
           gap-width     20
           border-radius 3
           class         "pause-symbol"}
    :as   opts}]
  (let [[origin-x origin-y] origin
        y               (- origin-y (/ height 2))
        offset-x-origin (- origin-x (/ bar-width 2))
        half-gap        (/ gap-width 2)
        attrs           {:y  y :width bar-width :height height
                         :rx border-radius :ry border-radius}]
    [:g {:class class}
     [:rect (merge {:x (- offset-x-origin half-gap)} attrs)]
     [:rect (merge {:x (+ offset-x-origin half-gap)} attrs)]]))

(defn play-button []
  [:svg.play-button
   {:view-box "0 0 100 100"}
   [:circle.button-circle
    {:r  50
     :cx 50
     :cy 50}]
   [play-symbol
    {:origin       [55 50]
     :height       50
     :stroke-width 8}]])

(defn preview-container []
  (let [liked    (reagent/atom false)
        playing? (reagent/atom false)]
    (fn []
      [:div.preview-panel
       [:div.bar.social

        [re-com/h-box
         :gap "40px"
         :children [[re-com/md-icon-button
                     :md-icon-name (if-not @liked "zmdi-favorite-outline" "zmdi-favorite")
                     :on-click (e> (swap! liked not)) :class "favorite"]
                    [re-com/md-icon-button
                     :md-icon-name "zmdi-share" :tooltip "share" :class "share"]
                    [re-com/md-icon-button
                     :md-icon-name "zmdi-time" :tooltip "watch later" :class "watch-later"]]]
        [:div.edit
         "Edit on Vimsical"]]
       [:div.preview-container
        [:div.play-button-overlay
         [play-button]]
        [live-preview]]
       [re-com/h-box
        :class "bar timeline-container"
        :justify :center
        :align :center
        :gap "18px"
        :children [[:svg.play-pause
                    {:view-box "0 0 100 100"
                     :on-click (e> (swap! playing? not))}
                    (if-not @playing?
                      [play-symbol
                       {:origin       [50 50]
                        :height       100
                        :stroke-width 20}]
                      [pause-symbol
                       {:origin        [50 50]
                        :height        100
                        :bar-width     30
                        :gap-width     60
                        :border-radius 10}])]
                   (let [dur 1e2]
                     [:div.timeline
                      [:div.progress.left]
                      [:div.progress.passed]
                      [:div.playhead]
                      [:div.skimhead]])
                   [:div.speed-control
                    "1.5x"]]]]))
  )

;; temp
(def person-url
  "https://encrypted-tbn1.gstatic.com/images?q=tbn:ANd9GcRw8FAuvFJSJIcHOPrGy_yhrpuQTDJOKQEJbggJneasIvQT1YHvY-cBxQ")

(defn info-and-editor-container []
  (let [show-info? (reagent/atom true)
        desc       "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent ut leo sit amet mauris tempus luctus. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent ut leo sit amet mauris tempus luctus. Nullam placerat metus leo, et faucibus odio mattis a. Nulla ac quam magna. Aliquam vestibulum enim dolor, sit amet volutpat arcu egestas nec. Nullam bibendum volutpat ultricies. Nunc nec vehicula ante. Nam est massa, sollicitudin sit amet felis a, vestibulum viverra nunc. Mauris et tristique turpis, non eleifend nulla. Mauris commodo blandit justo id imperdiet. Phasellus varius eget turpis interdum sodales. Vestibulum ac efficitur metus.\n\nDuis rutrum vel felis eu fringilla. Integer cursus sem eu mauris pulvinar rutrum. Proin magna sapien, pretium in enim a, eleifend fringilla neque. Donec in placerat mi. Sed volutpat auctor ipsum, ac lacinia lorem consectetur eu. Curabitur sit amet faucibus enim. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Nam consectetur est sed sem varius, eget dignissim magna tincidunt. Nulla quis urna eu libero pharetra ultrices. Aliquam placerat, nibh eget ultricies pharetra, neque nulla efficitur urna, non pellentesque nisi ex sed ante. Donec varius enim quam, non iaculis neque malesuada ac. Maecenas pretium non mauris in dictum. Proin tincidunt malesuada fermentum. Etiam vitae mi nisi. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.\n\nQuisque pharetra magna ut velit semper vehicula. Pellentesque ultricies ex sed metus suscipit mollis. Nulla facilisi. Vestibulum eget fermentum purus, in consequat arcu. Phasellus bibendum elit eu dolor consectetur, consectetur pretium erat faucibus. Curabitur leo eros, interdum eu odio sit amet, interdum pretium ipsum. Praesent pellentesque, turpis in maximus efficitur, libero nulla accumsan urna, et facilisis neque risus eget quam. Vivamus vel eros vel ligula mattis commodo vel a ante. Praesent mollis est consequat tellus blandit luctus. Fusce fringilla neque convallis ex pulvinar auctor. Vivamus fermentum mattis tincidunt. In in ipsum magna. Praesent nec eleifend arcu. Curabitur vel nisi nec quam tempus condimentum.\n\nSed faucibus orci est, non tincidunt urna efficitur at. Proin bibendum fermentum nisl, a rutrum turpis euismod eget. Sed accumsan magna eget sodales rhoncus. Integer ut lectus et diam suscipit malesuada in quis elit. Nunc suscipit aliquet consectetur. Curabitur luctus varius nulla, ut molestie quam condimentum quis. Aliquam sit amet nulla sed sapien cursus ultrices in et ligula. Aliquam in diam quis magna congue elementum sed non quam. Nullam euismod nibh vel magna pretium sagittis. Praesent nulla elit, vulputate sed porta varius, placerat in est. Cras eget gravida mi. Sed metus nibh, tincidunt sed interdum ac, sodales nec purus. Cras euismod finibus urna, ac ultrices velit posuere vitae. Aliquam erat volutpat. Vivamus pellentesque, lorem a gravida fermentum, arcu ipsum tristique nisl, ut rutrum felis lectus vel ante. Quisque mattis suscipit magna, fringilla euismod arcu rutrum vel. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent ut leo sit amet mauris tempus luctus. Nullam placerat metus leo, et faucibus odio mattis a. Nulla ac quam magna. Aliquam vestibulum enim dolor, sit amet volutpat arcu egestas nec. Nullam bibendum volutpat ultricies. Nunc nec vehicula ante. Nam est massa, sollicitudin sit amet felis a, vestibulum viverra nunc. Mauris et tristique turpis, non eleifend nulla. Mauris commodo blandit justo id imperdiet. Phasellus varius eget turpis interdum sodales. Vestibulum ac efficitur metus.\n\nDuis rutrum vel felis eu fringilla. Integer cursus sem eu mauris pulvinar rutrum. Proin magna sapien, pretium in enim a, eleifend fringilla neque. Donec in placerat mi. Sed volutpat auctor ipsum, ac lacinia lorem consectetur eu. Curabitur sit amet faucibus enim. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Nam consectetur est sed sem varius, eget dignissim magna tincidunt. Nulla quis urna eu libero pharetra ultrices. Aliquam placerat, nibh eget ultricies pharetra, neque nulla efficitur urna, non pellentesque nisi ex sed ante. Donec varius enim quam, non iaculis neque malesuada ac. Maecenas pretium non mauris in dictum. Proin tincidunt malesuada fermentum. Etiam vitae mi nisi. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus.\n\nQuisque pharetra magna ut velit semper vehicula. Pellentesque ultricies ex sed metus suscipit mollis. Nulla facilisi. Vestibulum eget fermentum purus, in consequat arcu. Phasellus bibendum elit eu dolor consectetur, consectetur pretium erat faucibus. Curabitur leo eros, interdum eu odio sit amet, interdum pretium ipsum. Praesent pellentesque, turpis in maximus efficitur, libero nulla accumsan urna, et facilisis neque risus eget quam. Vivamus vel eros vel ligula mattis commodo vel a ante. Praesent mollis est consequat tellus blandit luctus. Fusce fringilla neque convallis ex pulvinar auctor. Vivamus fermentum mattis tincidunt. In in ipsum magna. Praesent nec eleifend arcu. Curabitur vel nisi nec quam tempus condimentum.\n\nSed faucibus orci est, non tincidunt urna efficitur at. Proin bibendum fermentum nisl, a rutrum turpis euismod eget. Sed accumsan magna eget sodales rhoncus. Integer ut lectus et diam suscipit malesuada in quis elit. Nunc suscipit aliquet consectetur. Curabitur luctus varius nulla, ut molestie quam condimentum quis. Aliquam sit amet nulla sed sapien cursus ultrices in et ligula. Aliquam in diam quis magna congue elementum sed non quam. Nullam euismod nibh vel magna pretium sagittis. Praesent nulla elit, vulputate sed porta varius, placerat in est. Cras eget gravida mi. Sed "
        ]
    (reagent/create-class
     {:render
      (fn [c]
        [:div.info-and-editor-panel
         {:on-mouse-enter (e>
                           (reset! show-info? true))
          :on-mouse-out   (e>
                           (when-not (util.dom/view-contains-related-target? c e)
                             (reset! show-info? false)))}
         [:div.info
          {:class (when-not @show-info? "pan-out")}
          [:div.header
           [user/avatar
            {:img-url person-url
             :size    "50px"}]
           [:div.title-and-creator
            [:div.title.truncate "Pinterest CSS Layout And so much more"]
            [:div.creator.truncate "Jane Blacksmith"]]]
          (when desc)
          [:div.desc
           desc]]
         [code-editor {:file-type :html
                       :compact?  true
                       ;:read-only? true
                       }]
         [:div.logo-and-file-type.bar
          [icons/logo-and-type]
          ;; todo dynamic
          [:div.active-file-type.css
           ;; todo dynamic
           "HTML"]]])})))

(defn resizer
  "Consists of a line and an invisible stretch element to widen the draggable
  area without taking up space."
  []
  [:div.resizer
   [:div.stretcher]
   [re-com/line
    :class "divider-line"]])

(defn player []
  [:div.vimsical-frontend-player
   [splits/n-h-split
    :panels
    [[preview-container]
     [info-and-editor-container]]
    :splitter-size "1px"
    :splitter-child [resizer]
    :initial-split 70
    :margin "0"]])