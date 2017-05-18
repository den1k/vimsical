(ns vimsical.backend.handlers.status
  (:require [vimsical.backend.handlers.multi :refer [handle]]
            [vimsical.remotes.backend :as backend]))

(defmethod handle ::backend/status
  [context _]
  (assoc context :response
         {:status 200
          :body   [::backend/status-response :ok]}))
