(ns vimsical.backend.events.multifn)

(defmulti handle
  (fn [context [id]] id))
