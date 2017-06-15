(ns vimsical.vcs.edit-event
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::op #{:str/ins :str/rem :str/rplc :crsr/mv :crsr/sel})
(s/def ::amt pos-int?)
(s/def ::idx nat-int?)
(s/def ::start nat-int?)
(s/def ::end nat-int?)
(s/def ::range (s/tuple ::start ::end))
(s/def ::diff string?)

(defmulti edit-event-spec ::op)
(defmethod edit-event-spec :str/ins  [_] (s/keys :req [::op ::idx ::diff]))
(defmethod edit-event-spec :str/rem  [_] (s/keys :req [::op ::idx ::amt]))
(defmethod edit-event-spec :str/rplc [_] (s/keys :req [::op ::idx ::amt ::diff]))
(defmethod edit-event-spec :crsr/mv  [_] (s/keys :req [::op ::idx]))
(defmethod edit-event-spec :crsr/sel [_] (s/keys :req [::op ::range]))

(s/def ::edit-event (s/multi-spec edit-event-spec ::op))

(defn cursor-event? [{::keys [op]}]
  (= "crsr" (namespace op)))

(defmulti prospective-idx-offset ::op)
(defmethod prospective-idx-offset :str/ins [{::keys [diff]}] (count diff))
;; Deletions in the VCS's internal datastructures happened left to right, with
;; the cursor sitting left of the char(s) to be deleted. This means that the
;; cursor position should remain the same after performing the delete operation
(defmethod prospective-idx-offset :str/rem [_] 0)
