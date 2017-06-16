(ns vimsical.vcs.lib
  (:require
   [clojure.spec.alpha :as s]))

;; * Spec

(s/def ::lib
  (s/keys :req [::src ::type ::sub-type] :opt [::name]))


;; ** Attributes

(def sub-types #{:css :javascript})

(s/def ::src string?)
(s/def ::type #{:text})
(s/def ::sub-type sub-types)
(s/def ::name string?)


;;
;; * Helpers
;;

(defn sub-type= [{::keys [sub-type]} sb-type] (= sub-type sb-type))
(defn javascript? [lib] (sub-type= lib :javascript))

;;
;; * Constructor
;;

(s/fdef new-lib
        :args (s/cat :sub-type ::sub-type :src ::src :name (s/? ::name))
        :ret ::lib)

(defn new-lib
  ([sub-type src] {::src src ::type :text ::sub-type sub-type})
  ([sub-type src name] {::name name ::src src ::type :text ::sub-type sub-type}))
