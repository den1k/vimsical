(ns vimsical.frontend.code-editor.handlers
  (:require
   [re-frame.core :as re-frame]
   [re-frame.loggers :refer [console]]
   [vimsical.common.util.core :as util :include-macros true]
   [vimsical.frontend.code-editor.interop :as interop]
   [vimsical.frontend.code-editor.subs :as subs]
   [vimsical.frontend.code-editor.ui-db :as ui-db]
   [vimsical.frontend.code-editor.util :as code-editor.util]
   [vimsical.frontend.timeline.subs :as timeline.subs]
   [vimsical.frontend.util.re-frame :as util.re-frame :refer [<sub]]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.vcs.file :as file]
   [vimsical.vims :as vims]))

(defn- set-model-language [file editor-instance]
  #?(:cljs
     (let [model (.-model editor-instance)]
       (.. js/monaco -editor (setModelLanguage model (interop/file-lang file))))))

;;
;; * Model listeners
;;

;; XXX use interceptors to parse events?
(defn handle-content-change [vims model file e]
  (re-frame/dispatch [::content-change vims file e]))

(defn handle-cursor-change [vims model file e]
  (re-frame/dispatch [::cursor-change vims file e]))

(defn editor-focus-handler [vims file editor]
  (fn [_]
    (re-frame/dispatch [::focus vims file editor])))

(defn editor-blur-handler [vims file editor]
  (fn [_]
    (re-frame/dispatch [::blur vims file editor])))

(defn editor-mouse-down-handler [vims]
  (fn [_]
    ;; fully qualified ns to avoid circular dep with vcr.handlers
    (re-frame/dispatch [:vimsical.frontend.vcr.handlers/pause vims])))

(defn new-listeners
  [vims file editor]
  {:pre [vims file editor]}
  {:model->content-change-handler (fn model->content-change-handler [model]
                                    (fn [e]
                                      (handle-content-change vims model file e)))
   :model->cursor-change-handler  (fn model->cursor-change-handler [model]
                                    (fn [e]
                                      (handle-cursor-change vims model file e)))
   :editor->focus-handler         (partial editor-focus-handler vims file)
   :editor->blur-handler          (partial editor-blur-handler vims file)
   :editor->mouse-down-handler    (editor-mouse-down-handler vims)})


;;
;; * Instance Lifecycle
;;

(re-frame/reg-event-fx
 ::register
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ {:keys [vims file] :as opts} editor-instance]]
   {:ui-db (ui-db/set-editor ui-db vims file editor-instance)}))

(re-frame/reg-event-fx
 ::init
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub
   (fn [[_ {:keys [vims file]}]]
     ^:ignore-warnings [::subs/playhead-string vims file]))]
 (fn [{:keys       [ui-db]
       ::subs/keys [playhead-string]} [_ {:keys [vims file] :as opts}]]
   (let [editor    (ui-db/get-editor ui-db vims file)
         _         (set-model-language file editor)
         listeners (new-listeners vims file editor)]
     {:ui-db      (ui-db/set-listeners ui-db vims file listeners)
      :dispatch-n [[::set-string vims nil file playhead-string]
                   [::bind-listeners vims file]
                   [::track-start vims file]]})))

(re-frame/reg-event-fx
 ::handover
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_
                       {old-vims :vims old-file :file :as old-opts}
                       {new-vims :vims new-file :file :as new-opts}]]
   (let [editor-instance (ui-db/get-editor ui-db old-vims old-file)]
     {:ui-db (-> ui-db
                 (ui-db/set-editor old-vims old-file nil)
                 (ui-db/set-editor new-vims new-file editor-instance))})))

(re-frame/reg-event-fx
 ::recycle
 (fn [_ [_
         {old-vims :vims old-file :file :as old-opts}
         {new-vims :vims new-file :file :as new-opts}]]
   (when (and old-vims (not (util/=by :db/uid old-vims new-vims)))
     {:dispatch-n
      [[::clear-disposables old-vims old-file]
       [::track-stop old-vims old-file]
       [::handover old-opts new-opts]
       [::init new-opts]]})))

(re-frame/reg-event-fx
 ::dispose
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ {:keys [vims file]}]]
   (interop/dispose-editor (ui-db/get-editor ui-db vims file))
   {:dispatch-n [[::clear-disposables vims file]
                 [::track-stop vims file]]}))

;;
;; * Listeners lifecycle
;;

(re-frame/reg-event-fx
 ::clear-disposables
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ vims file]]
   #?(:cljs
      (if-some [disposables (ui-db/get-disposables ui-db vims file)]
        (do
          ;; Clear disposables
          (reduce-kv
           (fn [_ k disposable]
             (.dispose disposable))
           nil disposables)
          {:ui-db (ui-db/set-disposables ui-db vims file nil)})
        (console :error "disposables not found")))))

(re-frame/reg-event-fx
 ::bind-listeners
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ vims file]]
   #?(:cljs
      (when-some [editor (ui-db/get-editor ui-db vims file)]
        (when-some [listeners (ui-db/get-listeners ui-db vims file)]
          ;; Create new disposables and update ui-db
          (let [listeners' (interop/bind-listeners editor listeners)]
            {:ui-db (ui-db/set-disposables ui-db vims file listeners')}))))))

;;
;; * Linting
;;

(defn severity-code [flag]
  (case flag
    :error 3
    :warning 2
    :info 1
    :ignore 0))

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
 ::set-error-markers
 [(util.re-frame/inject-sub [::subs/editor-instance-for-subtype :javascript])]
 (fn [{:keys     [ui-db]
       js-editor ::subs/editor-instance-for-subtype} [_ errors]]
   (let [model   (.-model js-editor)
         markers (map model-marker errors)]
     (code-editor.util/set-model-markers model :javascript markers)
     nil)))

(re-frame/reg-event-fx
 ::clear-error-markers
 [(util.re-frame/inject-sub [::subs/editor-instance-for-subtype :javascript])]
 (fn [{:keys     [ui-db]
       js-editor ::subs/editor-instance-for-subtype} _]
   (let [model (.-model js-editor)]
     (code-editor.util/set-model-markers model :javascript [])
     nil)))

;;
;; ** User actions
;;


(re-frame/reg-event-fx
 ::content-change
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ vims file e]]
   (let [editor     (ui-db/get-editor ui-db vims file)
         model      (.-model editor)
         edit-event (interop/parse-content-event model e)]
     {:dispatch [::vcs.handlers/add-edit-event vims file edit-event]})))

(re-frame/reg-event-fx
 ::cursor-change
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ vims file e]]
   (let [editor     (ui-db/get-editor ui-db vims file)
         model      (.-model editor)
         edit-event (interop/parse-selection-event model e)]
     {:dispatch [::vcs.handlers/add-edit-event vims file edit-event]})))

(re-frame/reg-event-fx
 ::focus
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db] :as cofx} [_ vims file]]
   {:ui-db (ui-db/set-active-file ui-db vims file)}))

(re-frame/reg-event-fx
 ::blur
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db] :as cofx} [_ vims file]]
   {:ui-db (ui-db/set-active-file ui-db vims nil)}))

(re-frame/reg-event-fx
 ::paste
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ vims string]]
   #?(:cljs
      (let [editor (ui-db/get-active-editor ui-db vims)
            model  (.-model editor)
            sels   (.. editor -cursor getSelections)]
        (.pushEditOperations
         model
         sels
         (clj->js
          [{;; if true moves cursor, else makes selection around added text
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
   [{:keys [ui-db]} [_ vims read-only? file string]]
   #?(:cljs
      (if-some [editor (ui-db/get-editor ui-db vims file)]
        (do (.setValue editor string) nil)
        (console :error "editor not found")))))

(re-frame/reg-event-fx
 ::set-position
 [(re-frame/inject-cofx :ui-db)]
 (fn set-position
   [{:keys [ui-db]} [_ vims file position]]
   #?(:cljs
      (if-some [editor (ui-db/get-editor ui-db vims file)]
        (when-some [js-pos (some-> position interop/pos->js-pos)]
          (.revealRange editor js-pos)
          (.setSelection editor js-pos)
          (.focus editor)
          nil)
        (console :error "editor not found")))))

(re-frame/reg-event-fx
 ::update-editor-string
 (fn update-editor
   [_ [_ vims {file-uid :db/uid :as file} string]]
   (when (some? string)
     {:dispatch-n
      [[::clear-disposables vims file]
       [::set-string vims false file string]
       [::bind-listeners vims file]]})))

(re-frame/reg-event-fx
 ::update-editor-position
 [(util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/timeline-entry vims]))]
 (fn update-editor
   [{::vcs.subs/keys [timeline-entry]}
    [_ vims {file-uid :db/uid :as file} position]]
   (when (and (some? position) (= file-uid (:file-uid (second timeline-entry))))
     {:dispatch-n
      [[::clear-disposables vims file]
       [::set-position vims file position]
       [::bind-listeners vims file]]})))

(re-frame/reg-event-fx
 ::track-start
 (fn [_ [_ vims {file-uid :db/uid :as file}]]
   {:track
    [{:id           [::editor-str file-uid]
      :action       :register
      :subscription [::subs/string vims file]
      :val->event   (fn [string] [::update-editor-string vims file string])}
     {:id              [::editor-pos file-uid]
      :action          :register
      ;; Prevents showing cursors on all editors when reloading
      :dispatch-first? false
      :subscription    [::subs/position vims file]
      :val->event      (fn [position] [::update-editor-position vims file position])}]}))

(re-frame/reg-event-fx
 ::track-stop
 (fn [_ [_ vims {file-uid :db/uid}]]
   {:track
    [{:id [::editor-str file-uid] :action :dispose}
     {:id [::editor-pos file-uid] :action :dispose}]}))

(re-frame/reg-event-fx
 ::reset-editor-to-playhead
 [(util.re-frame/inject-sub
   (fn [[_ vims file]] ^:ignore-warnings [::subs/playhead-string vims file]))
  (util.re-frame/inject-sub
   (fn [[_ vims file]] ^:ignore-warnings [::subs/playhead-position vims file]))]
 (fn [{::subs/keys [playhead-string playhead-position]} [_ vims file set-position?]]
   {:dispatch-n
    (cond-> [[::clear-disposables vims file]
             [::set-string vims nil file playhead-string]]
      set-position? (conj [::set-position vims file playhead-position])
      true (conj [::bind-listeners vims file]))}))

(re-frame/reg-event-fx
 ::reset-all-editors-to-playhead
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub ^:ignore-warnings (fn [[_ vims]] [::vcs.subs/files vims]))
  (util.re-frame/inject-sub ^:ignore-warnings (fn [[_ vims]] [::vcs.subs/playhead-entry vims]))]
 (fn [{:keys           [ui-db]
       ::vcs.subs/keys [files playhead-entry]} [_ vims]]
   (let [[_ {:keys [file-uid]}] playhead-entry]
     {:dispatch-n
      (for [file files
            :when (ui-db/get-editor ui-db vims file)
            :let [set-pos? (= (:db/uid file) file-uid)]]
        [::reset-editor-to-playhead vims file set-pos?])})))
