(ns vimsical.backend.components.session-store.spec
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.validation :as vcs.validation]
   [vimsical.user :as user]))

(s/def ::empty-session empty?)

(s/def ::active-session
  (s/keys :req [::user/uid]))

(s/def ::vims-session
  (s/merge
   ::active-session
   (s/keys :req [::vcs.validation/delta-by-branch-uid])))

(s/def ::session
  (s/or :empty ::empty-session
        :active ::active-session
        :vims ::vims-session))
