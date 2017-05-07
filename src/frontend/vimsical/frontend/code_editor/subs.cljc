(ns vimsical.frontend.code-editor.subs
  (:require
   [re-frame.core :as re-frame]
   [re-frame.interop :as interop]
   [vimsical.frontend.util.re-frame :as util.re-frame :refer [<sub]]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.core :as vcs]))

(re-frame/reg-sub-raw
 ::file-string
 (fn [vcs [_ {file-id :db/id}]]
   (interop/make-reaction
    #(let [vcs                (<sub [::vcs.subs/vcs])
           [_ {delta-id :id}] (<sub [::vcs.subs/timeline-entry])]
       (vcs/file-string vcs file-id delta-id)))))

(re-frame/reg-sub-raw
 ::file-cursor
 (fn [vcs [_ {file-id :db/id}]]
   (interop/make-reaction
    #(let [vcs                (<sub [::vcs.subs/vcs])
           [_ {delta-id :id}] (<sub [::vcs.subs/timeline-entry])]
       (vcs/file-cursor vcs file-id delta-id)))))
