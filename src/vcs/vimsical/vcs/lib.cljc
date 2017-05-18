(ns vimsical.vcs.lib
  (:require
   [clojure.spec :as s]))

;; * Spec

(s/def ::lib
  (s/keys :req [:db/uid ::src ::type ::sub-type]
          :opt [::title]))              ; libs have titles, files have names


;; ** Attributes

(def sub-types #{:css :javascript})

(s/def ::uid uuid?)
(s/def :db/uid ::uid)
(s/def ::src string?)
(s/def ::type #{:text})
(s/def ::sub-type sub-types)
(s/def ::title string?)


;;
;; * Helpers
;;

(defn sub-type= [{::keys [sub-type]} sb-type] (= sub-type sb-type))
(defn javascript? [lib] (sub-type= lib :javascript))
