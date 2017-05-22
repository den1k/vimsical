(ns vimsical.backend.handlers.status
  (:require
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.remotes.backend.status.queries :as status.queries]))

(defmethod multi/context-spec ::status.queries/status [_] any?)
(defmethod multi/handle-event ::status.queries/status [_ _] {:status :ok})
