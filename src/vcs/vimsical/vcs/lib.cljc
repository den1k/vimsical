(ns vimsical.vcs.lib
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]))

;; * Spec

(s/def ::lib
  (s/keys :req [::src ::type ::sub-type] :opt [::name]))


;; ** Attributes

(def sub-types #{:css :javascript})

(s/def ::src string?)
(s/def ::type #{:text})
(s/def ::sub-type sub-types)
(s/def ::name string?)
(s/def ::version string?)

;;
;; * Helpers
;;

(defn sub-type= [{::keys [sub-type]} sb-type] (= sub-type sb-type))
(defn javascript? [lib] (sub-type= lib :javascript))

;;
;; * Constructor
;;

(s/fdef new-lib
        :args (s/or :custom  (s/cat :sub-type ::sub-type :src ::src)
                    :catalog (s/cat :name ::name :version ::version :sub-type ::sub-type :src ::src))
        :ret ::lib)

(defn new-lib
  ([sub-type src] {::src src ::type :text ::sub-type sub-type})
  ([name version sub-type src] {::name name ::version version ::src src ::type :text ::sub-type sub-type}))
