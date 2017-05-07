(ns vimsical.frontend.vcs.db
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.state.timeline :as timeline]))


(s/def ::playhead-entry (s/nilable ::timeline/entry))
(s/def ::skimhead-entry (s/nilable ::timeline/entry))
