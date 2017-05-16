(ns vimsical.backend.events.status
  (:require
   [vimsical.backend.events.multifn :refer [handle]]
   [vimsical.remotes.backend :as backend]))

(defmethod handle ::backend/status
  [context _]
  [::backend/status-response :ok])
