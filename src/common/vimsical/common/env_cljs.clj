(ns vimsical.common.env-cljs
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.common.env :as env]))

(s/def ::boolean ::env/boolean)
(s/def ::int ::env/int)
(s/def ::long ::env/long)
(s/def ::double ::env/double)
(s/def ::ratio ::env/ratio)
(s/def ::file ::env/file)
(s/def ::string ::env/string)
(s/def ::keyword ::env/keyword)

(defmacro optional
  ([k] (env/optional k nil))
  ([k spec-or-conformer] (env/optional k spec-or-conformer)))

(defmacro required
  ([k] (env/required k nil))
  ([k spec-or-conformer] (env/required k spec-or-conformer)))
