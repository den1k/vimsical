(ns vimsical.frontend.code-editor.util)

(declare set-model-markers)

(defn define-util-fns []
  #?(:cljs
     (do

       ;; setModelMarkers overwrites all currently set markers.
       ;; Something in monaco's language parsers uses it on every code change.
       ;; To prevent monaco from calling it, we overwrite the function with
       ;; our own wrapper. The wrapper allows monaco to call the function only
       ;; if the languange is not javascript. Otherwise it's a no-op.
       (def model-marker-fn)
       (set! vimsical.frontend.code_editor.util.model_marker_fn
             js/monaco.editor.setModelMarkers)
       (defn set-model-markers [model sub-type markers]
         (model-marker-fn model (name sub-type) (into-array markers)))
       (set! js/monaco.editor.setModelMarkers
             (fn [model sub-type-string markers]
               (when (not= "javascript" sub-type-string)
                 (model-marker-fn model sub-type-string markers)))))))