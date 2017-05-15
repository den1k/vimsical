(ns vimsical.frontend.live-preview.ui-db)

(defn get-iframe [ui-db ui-reg-key] (get-in ui-db [ui-reg-key ::iframe]))
(defn set-iframe [ui-db ui-reg-key iframe] (assoc-in ui-db [ui-reg-key ::iframe] iframe))

(defn get-src-blob-url [ui-db ui-reg-key] (get-in ui-db [ui-reg-key ::src-blob-url]))
(defn set-src-blob-url [ui-db ui-reg-key url] (assoc-in ui-db [ui-reg-key ::src-blob-url] url))
