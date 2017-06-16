(ns vimsical.vcs.lib
  (:require
   [clojure.spec.alpha :as s]))

;; * Spec

(s/def ::lib
  (s/keys :req [::src ::type ::sub-type]
          :opt [::title]))              ; libs have titles, files have names


;; ** Attributes

(def sub-types #{:css :javascript})

(s/def ::src string?)
(s/def ::type #{:text})
(s/def ::sub-type sub-types)
(s/def ::title string?)


;;
;; * Helpers
;;

(defn sub-type= [{::keys [sub-type]} sb-type] (= sub-type sb-type))
(defn javascript? [lib] (sub-type= lib :javascript))

;;
;; * Constructor
;;

(defn new-lib
  ([src] {::src src ::type :text ::sub-type :javascript})
  ([name src] {::name name ::src src ::type :text ::sub-type :javascript}))
