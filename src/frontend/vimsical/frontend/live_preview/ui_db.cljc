(ns vimsical.frontend.live-preview.ui-db
  (:require [vimsical.common.util.core :as util]))

(defn get-iframe [ui-db] (get-in ui-db [::iframe]))
(defn set-iframe [ui-db iframe] (assoc-in ui-db [::iframe] iframe))
(defn remove-iframe [ui-db] (util/dissoc-in ui-db [::iframe]))

(defn get-src-blob-url [ui-db] (get-in ui-db [::src-blob-url]))
(defn set-src-blob-url [ui-db url] (assoc-in ui-db [::src-blob-url] url))
