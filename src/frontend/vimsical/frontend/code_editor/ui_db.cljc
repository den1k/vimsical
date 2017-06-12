(ns vimsical.frontend.code-editor.ui-db)

(defn path [{:keys [ui-key vims file]} k]
  {:pre [ui-key vims file]}
  [(:db/uid vims) (:db/uid file) ui-key k])

;;
;; * Editor Instance
;;

(defn get-editor [ui-db opts] (get-in ui-db (path opts ::editor)))
(defn set-editor [ui-db opts obj] (assoc-in ui-db (path opts ::editor) obj))

;;
;; * Listeners
;;

(defn get-listeners [ui-db opts] (get-in ui-db (path opts ::listeners)))
(defn set-listeners [ui-db opts obj] (assoc-in ui-db (path opts ::listeners) obj))

;;
;; * Disposables
;;

(defn get-disposables [ui-db opts] (get-in ui-db (path opts ::disposables)))
(defn set-disposables [ui-db opts obj] (assoc-in ui-db (path opts ::disposables) obj))

;;
;; * Active editor
;;

(defn get-active-file [ui-db {vims-uid :db/uid}] (get-in ui-db [vims-uid ::active-file]))
(defn set-active-file [ui-db {vims-uid :db/uid} file] (assoc-in ui-db [vims-uid ::active-file] file))

(defn get-active-editor [ui-db {:keys [vims] :as opts}]
  (if-some [file (get-active-file ui-db vims)]
    (get-editor ui-db opts)
    ui-db))
