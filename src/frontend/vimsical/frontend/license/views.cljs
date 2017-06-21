(ns vimsical.frontend.license.views
  (:require
   [vimsical.common.util.core :as util]
   [vimsical.frontend.app.subs :as app.subs]
   [vimsical.frontend.license.subs :as subs]
   [vimsical.frontend.util.dom :refer-macros [e>]]
   [vimsical.frontend.util.re-frame :refer [<sub]]))

(defn license []
  (let [vims        (<sub [::app.subs/vims [:db/uid]])
        license-str (<sub [::subs/license-string vims])]
    [:div.license
     {:on-click (e> (.stopPropagation e))}
     [:h3 "MIT License"]
     [:div license-str]]))
