(ns vimsical.frontend.live-preview.ui-db
  (:require [vimsical.common.util.core :as util :include-macros true]
            [clojure.spec.alpha :as s]))

(defn path [{:keys [ui-key vims]} k]
  {:pre [ui-key (:db/uid vims) k]}
  [(:db/uid vims) ui-key k])

(s/def :db/uid uuid?)
(s/def ::vims (s/keys :req [:db/uid]))
(s/def ::ui-key keyword?)

(s/fdef path
        :args (s/cat :opts (s/keys :req-un [::vims ::ui-key])
                     :k (s/or :keyword keyword?
                              :tuple (s/tuple keyword? uuid?)))
        :ret (every-pred vector? not-empty))

(defn get-iframe [ui-db opts] (get-in ui-db (path opts ::elem)))
(defn set-iframe [ui-db opts iframe] (assoc-in ui-db (path opts ::elem) iframe))
(defn remove-iframe [ui-db opts] (util/dissoc-in ui-db (path opts ::elem)))

(defn get-src-blob-url [ui-db opts] (get-in ui-db (path opts ::src-blob-url)))
(defn set-src-blob-url [ui-db opts url] (assoc-in ui-db (path opts ::src-blob-url) url))


(def error-catcher-id ::error-catcher)

(defn error-catcher-state [ui-db]
  (get ui-db error-catcher-id))

(def get-error-catcher
  (comp :elem error-catcher-state))

(defn set-error-catcher [ui-db iframe]
  (update ui-db error-catcher-id assoc :elem iframe))

(defn set-error-catcher-status [ui-db status]
  (assoc-in ui-db [error-catcher-id :status] status))

(defn remove-error-catcher [ui-db]
  (dissoc ui-db error-catcher-id))

(defn set-error-catcher-error [ui-db error]
  (assoc-in ui-db [error-catcher-id :error] error))

(def get-error-catcher-error
  (comp :error error-catcher-state))

(def get-error-catcher-src-blob-url
  (comp :src-blob-url error-catcher-state))

(defn set-error-catcher-src-blob-url [ui-db url]
  (assoc-in ui-db [error-catcher-id :src-blob-url] url))
