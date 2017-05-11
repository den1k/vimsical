(ns vimsical.frontend.timeline.ui-db
  (:require [clojure.spec :as s]))

(s/def ::playhead (s/nilable number?))
(s/def ::skimhead (s/nilable number?))
(s/def ::playing? boolean?)
(s/def ::skimming? boolean?)

(defn set-playing  [ui-db playing?]  (assoc ui-db ::playing? playing?))
(defn set-skimming [ui-db skimming?] (assoc ui-db ::skimming? skimming?))
(defn set-playhead [ui-db t] (assoc ui-db ::playhead t))
(defn set-skimhead [ui-db t] (assoc ui-db ::skimhead t))
