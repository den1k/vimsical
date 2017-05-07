(ns vimsical.frontend.timeline.ui-db
  (:require
   [clojure.spec :as s]
   [re-frame.core :as re-frame]
   [vimsical.frontend.ui-db :as ui-db]))

(s/def ::playhead (s/nilable number?))
(s/def ::skimhead (s/nilable number?))

(re-frame/reg-sub ::playhead :<- [::ui-db/ui-db] ::playhead)
(re-frame/reg-sub ::skimhead :<- [::ui-db/ui-db] ::skimhead)
