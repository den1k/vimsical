(ns vimsical.vcs.compiler
  (:require
   [clojure.spec :as s]))

(def sub-types #{:babel})
(def to-sub-types #{:javascript})

;; * Attributes

(s/def ::id uuid?)
(s/def :db/id ::id)
(s/def ::type #{:text})
(s/def ::sub-type sub-types)
(s/def ::to-sub-type to-sub-types)
(s/def ::name string?)

;; * Entity
(s/def ::compiler
  (s/keys :req [:db/id ::type ::sub-type ::to-sub-type ::name]))
