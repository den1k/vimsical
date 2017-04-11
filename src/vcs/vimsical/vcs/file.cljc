(ns vimsical.vcs.file
  (:require
   [clojure.spec :as s]))


(def sub-types #{:html :css :javascript})

;; * Attributes

(s/def :db/uuid uuid?)
(s/def ::type #{:text})
(s/def ::sub-type sub-types)
(s/def ::name string?)

;; * Entity
(s/def ::file (s/keys :req [:db/uuid ::type ::sub-type] :opt [::name]))
