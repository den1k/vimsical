(ns vimsical.vcs.op
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::uid (s/nilable uuid?))
(s/def ::amt pos-int?)
(s/def :vimsical.vcs.op.str/ins    (s/tuple #{:str/ins} ::uid string?))
(s/def :vimsical.vcs.op.str/rem    (s/tuple #{:str/rem} ::uid ::amt))
(s/def :vimsical.vcs.op.crsr/move  (s/tuple #{:crsr/mv} ::uid))
(s/def :vimsical.vcs.op.crsr/sel   (s/tuple #{:crsr/sel} (s/tuple ::uid ::uid)))

(s/def ::op
  (s/or
   :str/ins   :vimsical.vcs.op.str/ins
   :str/rem   :vimsical.vcs.op.str/rem
   :crsr/move :vimsical.vcs.op.crsr/move
   :crsr/sel  :vimsical.vcs.op.crsr/sel))
