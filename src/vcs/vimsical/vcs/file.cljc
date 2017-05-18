(ns vimsical.vcs.file
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.compiler :as compiler]))


(def sub-types #{:html :css :javascript})

;;
;; * Attributes
;;

(s/def ::uid uuid?)
(s/def :db/uid ::uid)
(s/def ::type #{:text})
(s/def ::sub-type sub-types)
(s/def ::name string?)
(s/def ::compiler ::compiler/compiler)
(s/def ::lang-version string?)

;;
;; * Entity
(s/def ::file (s/keys :req [:db/uid ::type ::sub-type]
                      :opt [::name ::compiler ::lang-version]))

;;
;; * Helpers
;;

(defn sub-type= [{::keys [sub-type]} sb-type] (= sub-type sb-type))
(defn html? [file] (sub-type= file :html))
(defn css? [file] (sub-type= file :css))
(defn javascript? [file] (sub-type= file :javascript))
