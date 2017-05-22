(ns vimsical.common.env-cljs
  #?(:clj (:require [clojure.spec :as s] [vimsical.common.env :as env])))

#?(:clj (s/def ::boolean ::env/boolean))
#?(:clj (s/def ::int ::env/int))
#?(:clj (s/def ::long ::env/long))
#?(:clj (s/def ::double ::env/double))
#?(:clj (s/def ::ratio ::env/ratio))
#?(:clj (s/def ::file ::env/file))
#?(:clj (s/def ::string ::env/string))
#?(:clj (s/def ::keyword ::env/keyword))

#?(:clj
   (defmacro optional
     ([k] (env/optional k nil))
     ([k spec-or-conformer] (env/optional k spec-or-conformer))))

#?(:clj
   (defmacro required
     ([k] (env/required k nil))
     ([k spec-or-conformer] (env/required k spec-or-conformer))))
