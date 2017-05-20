(ns vimsical.vcs.lib
  (:require
   [clojure.spec :as s]))

;; * Spec

(s/def ::lib
  (s/keys :req [:db/id ::src ::type ::sub-type]
          :opt [::title]))              ; libs have titles, files have names


;; ** Attributes

(def sub-types #{:css :javascript})

(s/def ::id uuid?)
(s/def :db/id ::id)
(s/def ::src string?)
(s/def ::type #{:text})
(s/def ::sub-type sub-types)
(s/def ::title string?)


;;
;; * Helpers
;;

(defn sub-type= [{::keys [sub-type]} sb-type] (= sub-type sb-type))
(defn javascript? [lib] (sub-type= lib :javascript))
