(ns vimsical.remotes.backend.vims.commands
  (:require
   [clojure.spec :as s]
   [vimsical.remotes.event :as event]
   [vimsical.vims :as vims]
   [vimsical.vcs.snapshot :as snapshot]))

;;
;; * New
;;

(s/def ::new-vims (s/keys :req [:db/uid ::vims/owner ::vims/branches] :opt [::vims/title ::vims/cast]))
(defmethod event/event-spec    ::new [_] (s/cat :id any? :vims ::new-vims))
(defmethod event/result-spec ::new [_] any?)

;;
;; * Title
;;

(s/def ::title-vims (s/keys :req [::vims/tile]))
(defmethod event/event-spec    ::set-title! [_] (s/cat :id any? :vims ::title-vims))
(defmethod event/result-spec ::set-title! [_] any?)

;;
;; * Update snapshots
;;

(s/def ::vims-snapshots (s/every ::snapshot/snaphot))
(defmethod event/event-spec    ::update-snapshots [_] (s/cat :id any? :snapshots ::vims-snapshots))
(defmethod event/result-spec ::update-snapshots [_] any?)
