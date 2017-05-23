(ns vimsical.backend.components.session-store.spec
  (:require
   [clojure.spec :as s]
   [vimsical.backend.components.delta-store.validation
    :as
    delta-store.validation]
   [vimsical.user :as user]))

(s/def ::empty-session empty?)

(s/def ::active-session
  (s/keys :req [::user/uid]))

(s/def ::vims-session
  (s/merge
   ::active-session
   (s/keys :req [::delta-store.validation/deltas-by-branch-uid])))

(s/def ::session
  (s/or :empty ::empty-session
        :active ::active-session
        :vims ::vims-session))
