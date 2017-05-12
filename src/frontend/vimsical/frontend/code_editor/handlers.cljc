(ns vimsical.frontend.code-editor.handlers
  #?@(:clj
      [(:require
        [re-frame.core :as re-frame]
        [vimsical.vcs.edit-event :as edit-event]
        [vimsical.frontend.vcs.handlers :as vcs.handlers]
        [vimsical.frontend.util.re-frame :refer [<sub]]
        [vimsical.frontend.code-editor.ui-db :as ui-db]
        [vimsical.common.util.core :as util]
        [vimsical.frontend.code-editor.subs :as subs]
        [vimsical.frontend.code-editor.interop :as interop]
        [vimsical.frontend.util.re-frame :as util.re-frame]
        [vimsical.frontend.timeline.subs :as timeline.subs]
        [vimsical.frontend.vcr.subs :as vcr.subs]
        [vimsical.frontend.vcs.subs :as vcs.subs]
        [vimsical.vcs.core :as vcs])]
      :cljs
      [(:require
        [re-frame.core :as re-frame]
        [vimsical.vcs.edit-event :as edit-event]
        [vimsical.frontend.vcs.handlers :as vcs.handlers]
        [vimsical.frontend.util.re-frame :refer [<sub]]
        [re-frame.loggers :refer [console]]
        [vimsical.frontend.code-editor.ui-db :as ui-db]
        [vimsical.frontend.code-editor.subs :as subs]
        [vimsical.frontend.code-editor.interop :as interop]
        [vimsical.frontend.util.re-frame :as util.re-frame]
        [vimsical.frontend.timeline.subs :as timeline.subs]
        [vimsical.frontend.vcr.subs :as vcr.subs]
        [vimsical.frontend.vcs.subs :as vcs.subs]
        [vimsical.vcs.core :as vcs])]))

;;
;; * Instance lifecycle
;;

(re-frame/reg-event-fx
 ::register
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ file editor-instance listeners]]
   {:ui-db      (-> ui-db
                    (ui-db/set-editor file editor-instance)
                    (ui-db/set-listeners file listeners))
    :dispatch-n [[::set-string nil file ""]
                 [::bind-listeners file]
                 [::track-start file]]}))

(re-frame/reg-event-fx
 ::dispose
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db] :as cofx} [_ file]]
   (do
     ;; XXX fx?
     (interop/dispose-editor (ui-db/get-editor ui-db file))
     {:ui-db    (ui-db/set-editor ui-db file nil)
      :dispatch [::track-stop file]})))

;;
;; * Listeners lifecycle
;;

(re-frame/reg-event-fx
 ::clear-disposables
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ file]]
   #?(:cljs
      (if-some [disposables (ui-db/get-disposables ui-db file)]
        (do
          ;; Clear disposables
          (reduce-kv
           (fn [_ k disposable]
             (.dispose disposable))
           nil disposables)
          {:ui-db (ui-db/set-disposables ui-db file nil)})
        (console :error "disposables not found")))))

(re-frame/reg-event-fx
 ::bind-listeners
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ file]]
   #?(:cljs
      (when-some [editor (ui-db/get-editor ui-db file)]
        (when-some [listeners (ui-db/get-listeners ui-db file)]
          ;; Create new disposables and update ui-db
          (let [listeners' (interop/bind-listeners editor listeners)]
            {:ui-db (ui-db/set-disposables ui-db file listeners')}))))))

;;
;; * Linting
;;

(defn set-model-markers [model sub-type markers]
  #?(:cljs (.. js/monaco -editor (setModelMarkers model (name sub-type) markers))))

(defn severity-code [flag]
  (case flag
    :error   3
    :warning 2
    :info    1
    :ignore  0))

(defn model-marker
  [{:keys [pos msg severity]
    :or   {severity :error}}]
  #?(:cljs
     (let [{:keys [line col]} pos]
       #js {:message         msg
            :severity        (severity-code severity)
            :startColumn     col
            :startLineNumber line
            :endColumn       col
            :endLineNumber   line})))

(re-frame/reg-event-fx
 ::check-code-errors
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ {:keys [db/id] :as file}]]
   (when-let [errors (<sub [::vcs.subs/file-lint-or-preprocessing-errors file])]
     (let [editor  (ui-db/get-editor ui-db file)
           model   (.-model editor)
           markers (into-array (map model-marker errors))]
       ;; TODO fixme
       #?(:cljs
          (.setTimeout js/window
                       #(set-model-markers model
                                           :javascript
                                           markers)
                       1000))
       nil))))

;;
;; ** User actions
;;


(re-frame/reg-event-fx
 ::content-change
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ file e]]
   (let [editor     (ui-db/get-editor ui-db file)
         model      (.-model editor)
         edit-event (interop/parse-content-event model e)]
     {:dispatch-n [[::vcs.handlers/add-edit-event file edit-event]
                   [::check-code-errors file]]})))

(re-frame/reg-event-fx
 ::cursor-change
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ file e]]
   (let [editor     (ui-db/get-editor ui-db file)
         model      (.-model editor)
         edit-event (interop/parse-selection-event model e)]
     {:dispatch [::vcs.handlers/add-edit-event file edit-event]})))

(re-frame/reg-event-fx
 ::focus
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db] :as cofx} [_ file]]
   {:ui-db (ui-db/set-active-file ui-db file)}))

(re-frame/reg-event-fx
 ::blur
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db] :as cofx} [_ file]]
   {:ui-db (ui-db/set-active-file ui-db nil)}))

(re-frame/reg-event-fx
 ::paste
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ string]]
   #?(:cljs
      (let [editor (ui-db/get-active-editor ui-db)
            model  (.-model editor)
            sels   (.. editor -cursor getSelections)]
        (.pushEditOperations
         model
         sels
         (clj->js
          [{; if true moves cursor, else makes selection around added text
            :forceMoveMarkers true
            :text             string
            :range            (first sels)}]))))))

;;
;; ** Internal updates (from subscription)
;;

;; NOTE this will cause the .onContentDidChange callback to fire unless we
;; dispatch ::clear-disposables before
(re-frame/reg-event-fx
 ::set-string
 [(re-frame/inject-cofx :ui-db)]
 (fn set-string
   [{:keys [ui-db]} [_ read-only? file string]]
   #?(:cljs
      (if-some [editor (ui-db/get-editor ui-db file)]
        (do (.setValue editor string) nil)
        (console :error "editor not found")))))

(re-frame/reg-event-fx
 ::set-position
 [(re-frame/inject-cofx :ui-db)]
 (fn set-position
   [{:keys [ui-db]} [_ file position]]
   #?(:cljs
      (if-some [editor (ui-db/get-editor ui-db file)]
        (when-some [js-pos (some-> position interop/pos->js-pos)]
          (.revealRange editor js-pos)
          (.setSelection editor js-pos)
          (.focus editor)
          nil)
        (console :error "editor not found")))))

(re-frame/reg-event-fx
 ::update-editor-string
 (fn update-editor
   [_ [_ {file-id :db/id :as file} string]]
   (when (some? string)
     {:dispatch-n
      [[::clear-disposables file]
       [::set-string false file string]
       [::bind-listeners file]]})))

(re-frame/reg-event-fx
 ::update-editor-position
 (fn update-editor
   [_ [_ {file-id :db/id :as file} position]]
   (when (some? position)
     {:dispatch-n
      [[::clear-disposables file]
       [::set-position file position]
       [::bind-listeners file]]})))

(re-frame/reg-event-fx
 ::track-start
 (fn [_ [_ file]]
   {:track
    [{:id           [::editor-str file]
      :action       :register
      :subscription [::subs/string file]
      :val->event   (fn [string] [::update-editor-string file string])}
     {:id              [::editor-pos file]
      :action          :register
      ;; Prevents showing cursors on all editors when reloading
      :dispatch-first? false
      :subscription    [::subs/position file]
      :val->event      (fn [position] [::update-editor-position file position])}]}))

(re-frame/reg-event-fx
 ::track-stop
 (fn [_ [_ file]]
   {:track
    [{:id [::editor-str file]   :action :dispose}
     {:id [::editor-pos file] :action :dispose}]}))
