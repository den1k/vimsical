(ns vimsical.frontend.timeline.ui-db
  (:require [clojure.spec.alpha :as s]
            [vimsical.frontend.code-editor.ui-db :as code-editor.ui-db]))

(s/def ::playhead (s/nilable number?))
(s/def ::skimhead (s/nilable number?))
(s/def ::playing? boolean?)
(s/def ::skimming? boolean?)

(defn set-playing [ui-db {vims-uid :db/uid} playing?] {:pre [vims-uid]} (assoc-in ui-db [vims-uid ::playing?] playing?))
(defn set-skimming [ui-db {vims-uid :db/uid} skimming?] {:pre [vims-uid]} (assoc-in ui-db [vims-uid ::skimming?] skimming?))
(defn set-playhead [ui-db {vims-uid :db/uid :as vims} t] {:pre [vims-uid]}
  (-> ui-db
      (assoc-in [vims-uid ::playhead] t)
      (code-editor.ui-db/remove-no-history-strings vims)))
(defn set-skimhead [ui-db {vims-uid :db/uid :as vims} t] {:pre [vims-uid]}
  (-> ui-db
      (assoc-in [vims-uid ::skimhead] t)
      (code-editor.ui-db/remove-no-history-strings vims)))
