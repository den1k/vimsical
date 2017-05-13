(ns vimsical.frontend.live-preview.ui-db
  (:require [vimsical.common.util.core :as util]))

(defn get-iframe [ui-db] (get-in ui-db [::iframe :elem]))
(defn set-iframe [ui-db iframe] (assoc-in ui-db [::iframe :elem] iframe))
(defn remove-iframe [ui-db] (dissoc ui-db ::iframe))

(defn get-src-blob-url [ui-db] (get-in ui-db [::iframe :src-blob-url]))
(defn set-src-blob-url [ui-db url] (assoc-in ui-db [::iframe :src-blob-url] url))


(defn get-error-catcher [ui-db] (get-in ui-db [::error-catcher :elem]))
(defn set-error-catcher [ui-db iframe] (assoc-in ui-db [::error-catcher :elem] iframe))
(defn remove-error-catcher [ui-db] (dissoc ui-db ::error-catcher))

(defn set-error-catcher-error [ui-db error] (assoc-in ui-db [::error-catcher :error] error))
(defn get-error-catcher-error [ui-db] (get-in ui-db [::error-catcher :error]))

(defn get-error-catcher-src-blob-url [ui-db] (get-in ui-db [::error-catcher :src-blob-url]))
(defn set-error-catcher-src-blob-url [ui-db url] (assoc-in ui-db [::error-catcher :src-blob-url] url))
