(ns vimsical.frontend.code-editor.ui-db)

(defn path [{vims-uid :db/uid} {file-uid :db/uid} k]
  {:pre [vims-uid file-uid]}
  [vims-uid file-uid k])

;;
;; * Editor Instance
;;

(defn get-editor [ui-db vims file] (get-in ui-db (path vims file ::editor)))
(defn set-editor [ui-db vims file obj] (assoc-in ui-db (path vims file ::editor) obj))

;;
;; * Listeners
;;

(defn get-listeners [ui-db vims file] (get-in ui-db (path vims file ::listeners)))
(defn set-listeners [ui-db vims file obj] (assoc-in ui-db (path vims file ::listeners) obj))

;;
;; * Disposables
;;

(defn get-disposables [ui-db vims file] (get-in ui-db (path vims file ::disposables)))
(defn set-disposables [ui-db vims file obj] (assoc-in ui-db (path vims file ::disposables) obj))

;;
;; * Active editor
;;

(defn get-active-file [ui-db {vims-uid :db/uid}] (get-in ui-db [vims-uid ::active-file]))
(defn set-active-file [ui-db {vims-uid :db/uid} file] (assoc-in ui-db [vims-uid ::active-file] file))

;; When switching editors, the blur event of the old editor will happen after
;; the focus event of the second, so we need to compare and swap before removing
;; the active one

(defn unset-active-file [ui-db vims file]
  (if (= file (get-active-file ui-db vims))
    (set-active-file ui-db vims nil)
    ui-db))

(defn get-active-editor [ui-db vims]
  (if-some [file (get-active-file ui-db vims)]
    (get-editor ui-db vims file)
    ui-db))
