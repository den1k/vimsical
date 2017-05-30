(ns vimsical.frontend.vcr.subs
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.frontend.ui-db :as ui-db]
            [vimsical.frontend.vcr.ui-db :as vcr.ui-db]))

(re-frame/reg-sub
 ::file-hidden?
 :<- [::ui-db/ui-db]
 (fn [ui-db [_ file]]
   (vcr.ui-db/file-hidden? ui-db file)))

(re-frame/reg-sub
 ::visible-files
 (fn [[_ vims]]
   [(re-frame/subscribe [::ui-db/ui-db])
    (re-frame/subscribe [::vcs.subs/files vims])])
 (fn [[ui-db files]]
   (remove (partial vcr.ui-db/file-hidden? ui-db) files)))
