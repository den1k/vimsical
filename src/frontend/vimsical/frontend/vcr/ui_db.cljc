(ns vimsical.frontend.vcr.ui-db)

(defn file-hidden? [ui-db {file-uid :db/uid}]
  (get-in ui-db [file-uid ::hidden?]))

(defn toggle-file-visibility [ui-db {file-uid :db/uid}]
  (update-in ui-db [file-uid ::hidden?] not))
