(ns vimsical.frontend.code-editor.ui-db)

;;
;; * Editor Instance
;;

(defn editor-path [{file-id :db/id}] [::editor file-id])
(defn get-editor  [ui-db file]       (get-in ui-db (editor-path file)))
(defn set-editor  [ui-db file obj]   (assoc-in ui-db (editor-path file) obj))

;;
;; * Listeners
;;

(defn listeners-path  [{file-id :db/id}] [::listeners file-id])
(defn get-listeners   [ui-db file]       (get-in ui-db (listeners-path file)))
(defn set-listeners   [ui-db file obj]   (assoc-in ui-db (listeners-path file) obj))

;;
;; * Disposables
;;

(defn disposables-path [{file-id :db/id}] [::disposables file-id])
(defn get-disposables  [ui-db file]       (get-in ui-db (disposables-path file)))
(defn set-disposables  [ui-db file obj]   (assoc-in ui-db (disposables-path file) obj))
