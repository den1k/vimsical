(ns vimsical.backend.components.session-store.spec
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.validation :as vcs.validation]
   [vimsical.user :as user]
   [vimsical.vims :as vims]))

(s/def ::auth-session (s/keys :req [::user/uid]))
(s/def ::sync-state (s/every-kv ::vims/uid ::vcs.validation/delta-by-branch-uid))
(s/def ::sync-session (s/merge ::auth-session (s/keys :req [::sync-state])))

(s/def ::session
  (s/or :empty empty?
        :auth ::auth-session
        :sync ::sync-session))
