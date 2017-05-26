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
(defmethod event/event-spec  ::new [_] (s/cat :id any? :vims ::new-vims))
(defmethod event/result-spec ::new [_] (s/cat :id any? :result ::event/command-success))

;;
;; * Title
;;

(s/def ::title-vims (s/keys :req [:db/uid ::vims/title]))
(defmethod event/event-spec  ::title [_] (s/cat :id any? :vims ::title-vims))
(defmethod event/result-spec ::title [_] (s/cat :id any? :result ::event/command-success))

;;
;; * Update snapshots
;;

(s/def ::vims-snapshots
  (s/and
   (s/every ::snapshot/snapshot)
   (fn [snapshots]
     (apply = (map ::snapshot/vims-uid snapshots)))))
(defmethod event/event-spec  ::update-snapshots [_] (s/cat :id any? :snapshots ::vims-snapshots))
(defmethod event/result-spec ::update-snapshots [_] (s/cat :id any? :result ::event/command-success))
