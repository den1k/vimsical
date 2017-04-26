(ns vimsical.vcs.editor
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.edit-event :as edit-event]))

(s/def ::uuid-fn      (s/fspec :args (s/cat :edit-event ::edit-event/edit-event) :ret uuid?))
(s/def ::pad-fn       (s/fspec :args (s/cat :edit-event ::edit-event/edit-event) :ret nat-int?))
(s/def ::timestamp-fn (s/fspec :args (s/cat :edit-event ::edit-event/edit-event) :ret pos-int?))

(s/def ::effects (s/keys :req [::uuid-fn ::pad-fn ::timestamp-fn]))
