(ns vimsical.frontend.timeline.ui-db
  (:require [clojure.spec :as s]))

(s/def ::playhead (s/nilable number?))
(s/def ::skimhead (s/nilable number?))
(s/def ::playing? boolean?)
(s/def ::skimming? boolean?)

(defn set-playing [ui-db {vims-uid :db/uid} playing?] (assoc-in ui-db [vims-uid ::playing?] playing?))
(defn set-skimming [ui-db {vims-uid :db/uid} skimming?] (assoc-in ui-db [vims-uid ::skimming?] skimming?))
(defn set-playhead [ui-db {vims-uid :db/uid} t] (assoc-in ui-db [vims-uid ::playhead] t))
(defn set-skimhead [ui-db {vims-uid :db/uid} t] (assoc-in ui-db [vims-uid ::skimhead] t))
