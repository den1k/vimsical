(ns vimsical.user.settings.playback
  (:require
   [clojure.spec.alpha :as s]))

(s/def ::speed #{1.0 1.5 1.75 2.0 2.5})
