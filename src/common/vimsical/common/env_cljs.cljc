(ns vimsical.common.env-cljs
  #?(:clj (:require [vimsical.common.env :as env])))

#?(:clj
   (defmacro optional
     ([k] (env/optional k nil))
     ([k spec-or-conformer] (env/optional k spec-or-conformer))))

#?(:clj
   (defmacro required
     ([k] (env/required k nil))
     ([k spec-or-conformer] (env/required k spec-or-conformer))))
