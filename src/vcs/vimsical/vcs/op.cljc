(ns vimsical.vcs.op
  (:require
   [clojure.spec :as s]))

(s/def ::id (s/nilable uuid?))
(s/def ::amt pos-int?)
(s/def :vimsical.vcs.op.str/ins    (s/tuple #{:str/ins} ::id string?))
(s/def :vimsical.vcs.op.str/rem    (s/tuple #{:str/rem} ::id ::amt))
(s/def :vimsical.vcs.op.crsr/move  (s/tuple #{:crsr/mv} ::id))
(s/def :vimsical.vcs.op.crsr/sel   (s/tuple #{:crsr/sel} (s/tuple ::id ::id)))

(s/def ::op
  (s/or
   :str/ins   :vimsical.vcs.op.str/ins
   :str/rem   :vimsical.vcs.op.str/rem
   :crsr/move :vimsical.vcs.op.crsr/move
   :crsr/sel  :vimsical.vcs.op.crsr/sel))
