(ns vimsical.vcs.file
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.compiler :as compiler]))


(def sub-types #{:html :css :javascript})

;; * Attributes

(s/def ::id uuid?)
(s/def :db/id ::id)
(s/def ::type #{:text})
(s/def ::sub-type sub-types)
(s/def ::name string?)
(s/def ::compiler ::compiler/compiler)

;; * Entity
(s/def ::file (s/keys :req [:db/id ::type ::sub-type]
                      :opt [::name ::compiler]))
