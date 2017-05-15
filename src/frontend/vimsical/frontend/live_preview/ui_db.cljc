(ns vimsical.frontend.live-preview.ui-db
  (:require [vimsical.common.util.core :as util]))

(defn get-iframe [ui-db] (get-in ui-db [::iframe :elem]))
(defn set-iframe [ui-db iframe] (assoc-in ui-db [::iframe :elem] iframe))
(defn remove-iframe [ui-db] (dissoc ui-db ::iframe))

(defn get-src-blob-url [ui-db] (get-in ui-db [::iframe :src-blob-url]))
(defn set-src-blob-url [ui-db url] (assoc-in ui-db [::iframe :src-blob-url] url))


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
