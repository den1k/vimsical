(ns vimsical.frontend.code-editor.ui-db)

;;
;; * Editor Instance
;;

(defn editor-path [{file-uid :db/uid}] [::editor file-uid])
(defn get-editor  [ui-db file]       (get-in ui-db (editor-path file)))
(defn set-editor  [ui-db file obj]   (assoc-in ui-db (editor-path file) obj))

;;
;; * Listeners
;;

(defn listeners-path  [{file-uid :db/uid}] [::listeners file-uid])
(defn get-listeners   [ui-db file]       (get-in ui-db (listeners-path file)))
(defn set-listeners   [ui-db file obj]   (assoc-in ui-db (listeners-path file) obj))

;;
;; * Disposables
;;

(defn disposables-path [{file-uid :db/uid}] [::disposables file-uid])
(defn get-disposables  [ui-db file]       (get-in ui-db (disposables-path file)))
(defn set-disposables  [ui-db file obj]   (assoc-in ui-db (disposables-path file) obj))

;;
;; * Active editor
;;

(defn get-active-file   [ui-db]      (get-in ui-db [::active-file]))
(defn set-active-file   [ui-db file] (assoc-in ui-db [::active-file] file))

;; When switching editors, the blur event of the old editor will happen after
;; the focus event of the second, so we need to compare and swap before removing
;; the active one

(defn unset-active-file [ui-db file]
  (if (= file (get-active-file ui-db))
    (set-active-file ui-db nil)
    ui-db))

(defn get-active-editor [ui-db]
  (if-some [file (get-active-file ui-db)]
    (get-editor ui-db file)
    ui-db))
