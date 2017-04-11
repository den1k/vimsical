(ns vimsical.vcs.delta
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.op :as op]))

(s/def ::id uuid?)
(s/def ::prev-id (s/nilable ::id))
(s/def ::pad nat-int?)
(s/def ::branch-id uuid?)
(s/def ::file-id uuid?)
(s/def ::version number?)
(s/def ::meta (s/keys :req-un [:time/timestamp ::version]))

(s/def ::op
  (s/or
   :insert ::op/str-insert
   :remove ::op/str-remove
   :move ::op/crsr-move
   :sel ::op/crsr-sel))

(s/def ::delta
  (s/keys :req-un [::id ::prev-uuid
                   ::op
                   ::pad
                   ::file-id ::branch-id
                   ::meta]))

