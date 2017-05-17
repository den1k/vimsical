(ns vimsical.backend.handlers.status
  (:require [vimsical.backend.handlers.mutlifn :refer [handle]]
            [vimsical.remotes.backend :as backend]))

(defmethod handle ::backend/status
  [context _]
  [::backend/status-response :ok])
