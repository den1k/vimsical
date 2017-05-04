(ns vimsical.frontend.timeline.handlers
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.frontend.code-editor.handlers :as code-editor.handlers]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.state.timeline :as timeline]))

;;
;; * Db
;;

(defn get-skimhead   [vcs]     (get-in vcs   [::vcs/timeline ::timeline/skimhead] val))
(defn assoc-skimhead [vcs val] (assoc-in vcs [::vcs/timeline ::timeline/skimhead] val))

(defn get-playhead   [vcs]     (get-in vcs   [::vcs/timeline ::timeline/playhead] val))
(defn assoc-playhead [vcs val] (assoc-in vcs [::vcs/timeline ::timeline/playhead] val))

;;
;; * UI Db
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

;;
;; * Heads
;;

;; TODO integrate with playhead and the scheduler
(re-frame/reg-event-fx
 ::skimhead-start
 [(util.re-frame/inject-sub [::vcs.subs/vcs])]
 (fn [{:keys [ui-db db] ::vcs.subs/keys [vcs]} _]))

(re-frame/reg-event-fx
 ::skimhead-stop
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub [::vcs.subs/vcs])]
 (fn [{:keys [ui-db db] ::vcs.subs/keys [vcs]} _]))

(re-frame/reg-event-fx
 ::skimhead-set
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub [::vcs.subs/vcs])]
 (fn [{:keys                           [db ui-db]
       ::vcs.subs/keys [vcs]}
      [_ svg-node->timeline-position-fn]]
   (let [svg-node  (get ui-db ::svg)
         t         (svg-node->timeline-position-fn svg-node)
         delta     (vcs/timeline-delta-at-time vcs t)
         vcs' (-> vcs
                  (assoc-skimhead t)
                  (vcs/set-delta delta))]
     {:db       (mg/add db vcs')
      :dispatch [::code-editor.handlers/update-editors]})))

(re-frame/reg-event-fx
 ::skimhead-offset
 [(util.re-frame/inject-sub [::vcs.subs/vcs])
  (util.re-frame/inject-sub [::vcs.subs/timeline-duration])]
 (fn [{:keys           [db]
       ::vcs.subs/keys [vcs timeline-duration]}
      [_ dX]]
   (let [skimhead  (get-skimhead vcs)
         skimhead' (max 0 (min timeline-duration (+ skimhead dX)))
         delta     (vcs/timeline-delta-at-time vcs skimhead')
         vcs'      (-> vcs
                       (assoc-skimhead skimhead')
                       (vcs/set-delta delta))]
     {:db       (mg/add db vcs')
      :dispatch [::code-editor.handlers/update-editors]})))
