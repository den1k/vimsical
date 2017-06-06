(ns vimsical.vcs.file
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.common.util.core :as util]
   [vimsical.vcs.compiler :as compiler]))


(def sub-types #{:html :css :javascript})

;;
;; * Spec
;;

(s/def ::uid uuid?)
(s/def :db/uid ::uid)
(s/def ::type #{:text})
(s/def ::sub-type sub-types)
(s/def ::name string?)
(s/def ::compiler ::compiler/compiler)
(s/def ::lang-version string?)
(s/def ::file (s/keys :req [:db/uid ::type ::sub-type]
                      :opt [::name ::compiler ::lang-version]))

;;
;; * Helpers
;;

(defn sub-type= [{::keys [sub-type]} sb-type] (= sub-type sb-type))

(defn html?       [file] (sub-type= file :html))
(defn css?        [file] (sub-type= file :css))
(defn javascript? [file] (sub-type= file :javascript))

;;
;; * Constructor
;;

(defn new-file
  ([uid type sub-type] (new-file uid type sub-type nil nil))
  ([uid type sub-type lang-version compiler]
   (-> {:db/uid    uid
        ::type     type
        ::sub-type sub-type}
       (util/assoc-some ::lang-version lang-version ::compiler compiler))))
