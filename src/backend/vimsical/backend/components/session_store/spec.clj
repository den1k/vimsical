(ns vimsical.backend.components.session-store.spec
  (:require
   [clojure.spec :as s]
   [vimsical.user :as user]))

(s/def ::session
  (s/nilable (s/keys :opt [::user/id])))
