(ns vimsical.vcs.op
  (:require
   [clojure.spec :as s]))

;; NOTE Should match :vimsical.vcs.delta/id, but circular dependency
(s/def ::id (s/nilable nat-int?))
(s/def ::str-insert (s/tuple #{:str/ins} ::id string?))
(s/def ::str-remove (s/tuple #{:str/rem} ::id))
(s/def ::crsr-move  (s/tuple #{:crsr/mv} ::id))
(s/def ::crsr-sel   (s/tuple #{:crsr/sel} (s/tuple ::id ::id)))
