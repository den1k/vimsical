(ns vimsical.frontend.timeline.ui-db
  (:require [clojure.spec.alpha :as s]))

(s/def ::playhead (s/nilable number?))
(s/def ::skimhead (s/nilable number?))
(s/def ::playing? boolean?)
(s/def ::skimming? boolean?)

(defn set-playing [ui-db {vims-uid :db/uid} playing?] {:pre [vims-uid]} (assoc-in ui-db [vims-uid ::playing?] playing?))
(defn set-skimming [ui-db {vims-uid :db/uid} skimming?] {:pre [vims-uid]} (assoc-in ui-db [vims-uid ::skimming?] skimming?))
(defn set-playhead [ui-db {vims-uid :db/uid} t] {:pre [vims-uid]} (assoc-in ui-db [vims-uid ::playhead] t))
(defn set-skimhead [ui-db {vims-uid :db/uid} t] {:pre [vims-uid]}(assoc-in ui-db [vims-uid ::skimhead] t))
