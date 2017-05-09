(ns vimsical.frontend.vcs.db
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.state.timeline :as timeline]))

(s/def ::branch-id ::branch/id)
(s/def ::delta-id ::delta/prev-id)
(s/def ::playhead-entry (s/nilable ::timeline/entry))
(s/def ::skimhead-entry (s/nilable ::timeline/entry))
(s/def ::state (s/keys :req [::branch-id ::delta-id] :opt [::playhead-entry ::skimhead-entry]))
(s/def ::vcs (s/merge ::vcs/vcs ::state ))

(s/fdef get-playhead-entry :args (s/cat :vcs ::vcs) :ret ::playhead-entry)
(defn get-playhead-entry [vcs] (get vcs ::playhead-entry))

(s/fdef set-playhead-entry :args (s/cat :vcs ::vcs :entry ::playhead-entry) :ret ::vcs)
(defn set-playhead-entry [vcs entry] (assoc vcs ::playhead-entry entry))

(s/fdef get-skimhead-entry :args (s/cat :vcs ::vcs)  :ret ::skimhead-entry)
(defn get-skimhead-entry [vcs] (get vcs ::skimhead-entry))

(s/fdef set-skimhead-entry :args (s/cat :vcs ::vcs :entry ::skimhead-entry) :ret ::vcs)
(defn set-skimhead-entry [vcs entry] (assoc vcs ::skimhead-entry entry))
