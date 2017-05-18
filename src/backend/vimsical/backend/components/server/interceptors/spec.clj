(ns vimsical.backend.components.server.interceptors.spec
  (:require
   [clojure.spec :as s]))

(s/def ::request map?)
(s/def ::response (s/nilable map?))
(s/def ::context (s/keys :req-un [::request] :opt-un [::response]))
(s/def ::context-handler
  (s/fspec :args (s/cat :context ::context) :ret ::context))
