(ns vimsical.frontend.timeline.handlers
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.frontend.code-editor.handlers :as code-editor.handlers]
   [vimsical.frontend.timeline.subs :as subs]
   [vimsical.frontend.timeline.ui-db :as timeline.ui-db]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.vcs.db :as vcs.db]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.core :as vcs]))

;;
;; * UI Db
;;

;;
;; ** SVG
;;

(re-frame/reg-event-fx
 ::register-svg
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ node]]
   {:ui-db (assoc ui-db ::svg node)}))

(re-frame/reg-event-fx
 ::dispose-svg
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ node]]
   {:ui-db (dissoc ui-db ::svg node)}))

(defn ui-time
  [{:keys [ui-db]} svg-node->timeline-position-fn]
  (let [svg-node  (get ui-db ::svg)]
    (svg-node->timeline-position-fn svg-node)))

(defn ui-timeline-entry
  [{::vcs.subs/keys [vcs] :as cofx} svg-node->timeline-position-fn]
  (let [ui-time        (ui-time cofx svg-node->timeline-position-fn)
        timeline-entry (vcs/timeline-entry-at-time vcs ui-time)]
    [ui-time timeline-entry]))

;;
;; ** Skimhead
;;

(re-frame/reg-event-fx
 ::skimhead-start
 [(util.re-frame/inject-sub [::vcs.subs/vcs])]
 (fn [{:keys [ui-db db] ::vcs.subs/keys [vcs]} _]))

(re-frame/reg-event-fx
 ::skimhead-stop
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub [::vcs.subs/vcs])]
 (fn [{:keys [ui-db db] ::vcs.subs/keys [vcs]} _]
   (let [vcs'      (dissoc vcs ::vcs.db/skimhead-entry)
         db'       (mg/add db vcs')
         ui-db'    (dissoc ui-db ::timeline.ui-db/skimhead)]
     {:db       db'
      :ui-db    ui-db'
      :dispatch [::code-editor.handlers/update-editors]})))

(re-frame/reg-event-fx
 ::skimhead-set
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub [::vcs.subs/vcs])]
 (fn [{:as             cofx
       :keys           [db ui-db]
       ::vcs.subs/keys [vcs]}
      [_ svg-node->timeline-position-fn]]
   (let [[t entry] (ui-timeline-entry cofx svg-node->timeline-position-fn)
         vcs'      (assoc vcs ::vcs.db/skimhead-entry entry)
         db'       (mg/add db vcs')
         ui-db'    (assoc ui-db ::timeline.ui-db/skimhead t)]
     {:db       db'
      :ui-db    ui-db'
      :dispatch [::code-editor.handlers/update-editors]})))

(re-frame/reg-event-fx
 ::skimhead-offset
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub [::vcs.subs/vcs])
  (util.re-frame/inject-sub [::subs/duration])
  (util.re-frame/inject-sub [::subs/skimhead])]
 (fn [{:keys           [db ui-db]
       ::vcs.subs/keys [vcs]
       ::subs/keys     [skimhead duration]}
      [_ dx]]
   (let [skimhead' (max 0 (min duration (+ skimhead dx)))
         entry     (vcs/timeline-entry-at-time vcs skimhead')
         vcs'      (assoc vcs ::vcs.db/skimhead-entry entry)
         db'       (mg/add db vcs')
         ui-db'    (assoc ui-db ::timeline.ui-db/skimhead skimhead')]
     {:db       db'
      :ui-db    ui-db'
      :dispatch [::code-editor.handlers/update-editors]})))

;;
;; ** Playhead
;;

(re-frame/reg-event-fx
 ::playhead-set
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ t]]
   {:ui-db (assoc ui-db ::timeline.ui-db/playhead t)}))

(re-frame/reg-event-fx
 ::playhead-set-entry
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub [::vcs.subs/vcs])]
 (fn [{:as             cofx
       :keys           [db ui-db]
       ::vcs.subs/keys [vcs]}
      [_ svg-node->timeline-position-fn]]
   (let [[t entry] (ui-timeline-entry cofx svg-node->timeline-position-fn)
         vcs'      (assoc vcs ::vcs.db/playhead-entry entry)
         db'       (mg/add db vcs')
         ui-db'    (assoc ui-db ::timeline.ui-db/playhead t)]
     {:db       db'
      :ui-db    ui-db'
      :dispatch [::code-editor.handlers/update-editors]})))
