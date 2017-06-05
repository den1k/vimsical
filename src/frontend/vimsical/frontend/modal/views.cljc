(ns vimsical.frontend.modal.views
  (:require [vimsical.frontend.app.subs :as app.subs]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.frontend.vims-list.views :refer [vims-list]]))

(defn modal []
  (when-let [modal (<sub [::app.subs/modal])]
    [:div.modal-overlay.jc
     [:div.modal-container
      (case modal
        :modal/vims-list [vims-list])]]))
