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

;;
;; * Model listeners
;;

;; XXX use interceptors to parse events?
(defn handle-content-change [opts model e]
  (re-frame/dispatch [::content-change opts e]))

(defn handle-cursor-change [opts model e]
  (re-frame/dispatch [::cursor-change opts e]))

(defn editor-focus-handler [opts editor]
  (fn [_]
    (re-frame/dispatch [::focus opts editor])))

(defn editor-blur-handler [opts editor]
  (fn [_]
    (re-frame/dispatch [::blur opts editor])))

(defn editor-mouse-down-handler [{:keys [vims]}]
  (fn [_]
    ;; fully qualified ns to avoid circular dep with vcr.handlers
    (re-frame/dispatch [:vimsical.frontend.vcr.handlers/pause vims])))

(defn new-listeners
  [{:keys [vims file] :as opts} editor]
  {:pre [vims file editor]}
  {:model->content-change-handler (fn model->content-change-handler [model]
                                    (fn [e]
                                      (handle-content-change opts model e)))
   :model->cursor-change-handler  (fn model->cursor-change-handler [model]
                                    (fn [e]
                                      (handle-cursor-change opts model e)))
   :editor->focus-handler         (partial editor-focus-handler opts)
   :editor->blur-handler          (partial editor-blur-handler opts)
   :editor->mouse-down-handler    (editor-mouse-down-handler opts)})


;;
;; * Instance Lifecycle
;;

(re-frame/reg-event-fx
 ::register
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ opts editor-instance]]
   {:ui-db (ui-db/set-editor ui-db opts editor-instance)}))

(re-frame/reg-event-fx
 ::init
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub
   (fn [[_ {:keys [vims file] :as opts}]]
     ^:ignore-warnings [::subs/playhead-string vims file]))]
 (fn [{:keys       [ui-db]
       ::subs/keys [playhead-string]} [_ {:keys [vims file] :as opts}]]
   (let [editor    (ui-db/get-editor ui-db opts)
         _         (interop/set-model-language file editor)
         listeners (new-listeners opts editor)]
     {:ui-db      (ui-db/set-listeners ui-db opts listeners)
      :dispatch-n [[::set-string opts playhead-string]
                   [::bind-listeners opts]
                   [::track-start opts]]})))

(re-frame/reg-event-fx
 ::handover
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ old-opts new-opts]]
   (let [editor-instance (ui-db/get-editor ui-db old-opts)]
     {:ui-db (-> ui-db
                 (ui-db/set-editor old-opts nil)
                 (ui-db/set-editor new-opts editor-instance))})))

(re-frame/reg-event-fx
 ::recycle
 (fn [_ [_ {old-vims :vims :as old-opts} {new-vims :vims :as new-opts}]]
   (when (and old-vims (not (util/=by :db/uid old-vims new-vims)))
     {:dispatch-n
      [[::clear-disposables old-opts]
       [::track-stop old-opts]
       [::handover old-opts new-opts]
       [::init new-opts]]})))

(re-frame/reg-event-fx
 ::dispose
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ opts]]
   #?(:cljs
      (do
        (interop/dispose-editor (ui-db/get-editor ui-db opts))
        {:dispatch-n [[::clear-disposables opts]
                      [::track-stop opts]]}))))

;;
;; * Listeners lifecycle
;;

(re-frame/reg-event-fx
 ::clear-disposables
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ opts]]
   #?(:cljs
      (if-some [disposables (ui-db/get-disposables ui-db opts)]
        (do
          (interop/clear-disposables disposables)
          {:ui-db (ui-db/set-disposables ui-db opts nil)})
        (console :error "disposables not found")))))

(re-frame/reg-event-fx
 ::bind-listeners
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ opts]]
   #?(:cljs
      (when-some [editor (ui-db/get-editor ui-db opts)]
        (when-some [listeners (ui-db/get-listeners ui-db opts)]
          ;; Create new disposables and update ui-db
          (let [listeners' (interop/bind-listeners editor listeners)]
            {:ui-db (ui-db/set-disposables ui-db opts listeners')}))))))

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

;; NOTE we can filter out the :crsr/mv events that directly follow a :str/ins or
;; a :str/rem because the vcs builds up a "prospective" cursor value and the
;; cursor value of those deltas accounts for their diff, making the discrete
;; :crsr/mv event that follows redundant.

(defn moved-since?
  [prev-edit-event new-edit-event]
  (or (nil? prev-edit-event)
      (not= (+ (::edit-event/idx prev-edit-event)
               (edit-event/prospective-idx-offset prev-edit-event))
            (::edit-event/idx new-edit-event))))

(re-frame/reg-event-fx
 ::content-change
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ {:keys [vims file no-history?] :as opts} e]]
   #?(:cljs
      (let [editor (ui-db/get-editor ui-db opts)]
        (if no-history?
          {:ui-db (ui-db/set-no-history-string ui-db vims file (interop/get-value editor))}
          (let [model      (.-model editor)
                edit-event (interop/parse-content-event model e)]
            {:ui-db    (ui-db/set-last-edit-event ui-db vims file edit-event)
             :dispatch [::vcs.handlers/add-edit-event vims file edit-event]}))))))

(re-frame/reg-event-fx
 ::cursor-change
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ {:keys [vims file no-history?] :as opts} e]]
   #?(:cljs
      (when-not no-history?
        (let [editor     (ui-db/get-editor ui-db opts)
              model      (.-model editor)
              edit-event (interop/parse-selection-event model e)
              prev-event (ui-db/get-last-edit-event ui-db vims file)]
          (when (moved-since? prev-event edit-event)
            {:ui-db    (ui-db/set-last-edit-event ui-db vims file edit-event)
             :dispatch [::vcs.handlers/add-edit-event vims file edit-event]}))))))

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
 (fn [{:keys [ui-db]} [_ opts string]]
   #?(:cljs
      (let [editor (ui-db/get-active-editor ui-db opts)
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
   [{:keys [ui-db]} [_ opts string]]
   #?(:cljs
      (if-some [editor (ui-db/get-editor ui-db opts)]
        (do (interop/set-value editor string) nil)
        (console :error "editor not found")))))

(re-frame/reg-event-fx
 ::set-position
 [(re-frame/inject-cofx :ui-db)]
 (fn set-position
   [{:keys [ui-db]} [_ opts position]]
   #?(:cljs
      (if-some [editor (ui-db/get-editor ui-db opts)]
        (when-some [js-pos (some-> position interop/pos->js-pos)]
          (interop/reveal-range editor js-pos)
          (interop/set-selection editor js-pos)
          (interop/focus editor)
          nil)
        (console :error "editor not found")))))

(re-frame/reg-event-fx
 ::update-editor-string
 (fn update-editor
   [_ [_ opts string]]
   (when (some? string)
     {:dispatch-n
      [[::clear-disposables opts]
       [::set-string opts string]
       [::bind-listeners opts]]})))

(re-frame/reg-event-fx
 ::update-editor-position
 [(util.re-frame/inject-sub (fn [[_ {:keys [vims]}]] [::vcs.subs/timeline-entry vims]))]
 (fn update-editor
   [{::vcs.subs/keys [timeline-entry]}
    [_ {:keys [file] :as opts} position]]
   (let [file-uid (:db/uid file)]
     (when (and (some? position) (= file-uid (:file-uid (second timeline-entry))))
       {:dispatch-n
        [[::clear-disposables opts]
         [::set-position opts position]
         [::bind-listeners opts]]}))))

(re-frame/reg-event-fx
 ::set-read-only
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ opts bool]]
   #?(:cljs
      (let [editor (ui-db/get-editor ui-db opts)]
        (interop/update-options editor {:readOnly bool})))))

(re-frame/reg-event-fx
 ::track-start
 (fn [_ [_ {:keys [vims file] :as opts}]]
   {:track
    [{:id           (ui-db/path opts ::editor-str)
      :action       :register
      :subscription [::subs/string vims file]
      :val->event   (fn [string] [::update-editor-string opts string])}
     {:id              (ui-db/path opts ::editor-pos)
      :action          :register
      ;; Prevents showing cursors on all editors when reloading
      :dispatch-first? false
      :subscription    [::subs/position vims file]
      :val->event      (fn [position] [::update-editor-position opts position])}
     {:id           (ui-db/path opts ::branch-limit)
      :action       :register
      :subscription [::vcs.subs/branch-limit? vims]
      :val->event   (fn [limit?] [::set-read-only opts limit?])}]}))

(re-frame/reg-event-fx
 ::track-stop
 (fn [_ [_ opts]]
   {:track
    [{:id (ui-db/path opts ::editor-str) :action :dispose}
     {:id (ui-db/path opts ::editor-pos) :action :dispose}
     {:id (ui-db/path opts ::branch-limit) :action :dispose}]}))

(re-frame/reg-event-fx
 ::reset-editor-to-playhead
 [(util.re-frame/inject-sub
   (fn [[_ {:keys [vims file]}]] ^:ignore-warnings [::subs/playhead-string vims file]))
  (util.re-frame/inject-sub
   (fn [[_ {:keys [vims file]}]] ^:ignore-warnings [::subs/playhead-position vims file]))]
 (fn [{::subs/keys [playhead-string playhead-position]}
      [_ {:keys [vims file] :as opts} set-position?]]
   {:dispatch-n
    (cond-> [[::clear-disposables opts]
             [::set-string opts playhead-string]]
      set-position? (conj [::set-position opts playhead-position])
      true (conj [::bind-listeners opts]))}))

(re-frame/reg-event-fx
 ::reset-all-editors-to-playhead
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub
   ^:ignore-warnings
   (fn [[_ {:keys [vims]}]] [::vcs.subs/files vims]))
  (util.re-frame/inject-sub
   ^:ignore-warnings
   (fn [[_ {:keys [vims]}]] [::vcs.subs/playhead-entry vims]))]
 (fn [{:keys           [ui-db]
       ::vcs.subs/keys [files playhead-entry]} [_ opts]]
   (let [[_ {:keys [file-uid]}] playhead-entry]
     {:dispatch-n
      (for [file files
            :let [opts (assoc opts :file file)]
            :when (ui-db/get-editor ui-db opts)
            :let [set-pos? (= (:db/uid file) file-uid)]]
        [::reset-editor-to-playhead opts set-pos?])})))
