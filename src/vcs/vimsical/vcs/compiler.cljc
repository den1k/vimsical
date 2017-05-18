(ns vimsical.vcs.compiler
  (:require
   [clojure.spec :as s]))

(def sub-types #{:babel})
(def to-sub-types #{:javascript})

;; * Attributes

(s/def ::uid uuid?)
(s/def :db/uid ::uid)
(s/def ::type #{:text})
(s/def ::sub-type sub-types)
(s/def ::to-sub-type to-sub-types)
(s/def ::name string?)

;; * Entity
(s/def ::compiler
  (s/keys :req [:db/uid ::type ::sub-type ::to-sub-type ::name]))
