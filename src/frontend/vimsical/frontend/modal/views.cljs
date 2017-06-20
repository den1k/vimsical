(ns vimsical.frontend.modal.views
  (:require [vimsical.frontend.app.subs :as app.subs]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.frontend.vims-list.views :refer [vims-list]]
            [vimsical.frontend.share.views :refer [share]]
            [vimsical.frontend.license.views :refer [license]]
            [vimsical.frontend.vcr.libs.views :refer [libs]]
            [vimsical.user :as user]
            [vimsical.vims :as vims]))

(defn modal []
  (when-let [modal (<sub [::app.subs/modal])]
    [:div.modal-overlay.jc
     [:div.modal-container.dc.jc
      (case modal
        :modal/vims-list [vims-list]
        :modal/share [share]
        :modal/license [license]
        :modal/libs [libs])]]))
