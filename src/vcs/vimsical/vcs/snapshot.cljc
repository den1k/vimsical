(ns vimsical.vcs.snapshot
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.file :as file]))

(s/def ::user-uid uuid?)
(s/def ::vims-uid uuid?)
(s/def ::file-uid ::file/uid)
(s/def ::delta-uid (s/nilable ::delta/uid))
(s/def ::text string?)

;; NOTE the delta-uid should be nil for now because we want to keep a single
;; snapshot per file: the latest version in the master branch.

(s/def ::snapshot
  (s/keys :req [::user-uid ::vims-uid ::file-uid ::text] :opt [::delta-uid]))
