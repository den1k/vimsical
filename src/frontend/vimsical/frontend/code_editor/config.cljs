(ns vimsical.frontend.code-editor.config
  (:require [vimsical.frontend.util.dom :as util.dom]))

(defn configure-editor []
  ;; js config
  (doto js/monaco.languages.typescript.javascriptDefaults
    ;; validation
    (.setDiagnosticsOptions
     #js {:noSemanticValidation false
          :noSyntaxValidation   false})
    ;; compiler opts
    ;; https://github.com/Microsoft/monaco-editor/blob/3d17695d9322cb89dd2c9cee997807a6aadd7295/monaco.d.ts#L4948
    ;; https://www.typescriptlang.org/docs/handbook/compiler-options.html
    (.setCompilerOptions
     #js {:allowJs              true
          :target               js/monaco.languages.typescript.ScriptTarget.ES5
          ;:jsx ; "React", "Preserve", "None" ; default: preserve
          ;:jsxFactory ; default "React.createElement"
          :allowNonTsExtensions true})))



#_(let [model-uri (-> (js/monaco.editor.getModels) last .-uri)]
    (-> (js/monaco.languages.typescript.getJavaScriptWorker)
        (.then
         (fn [w]
           (-> (w model-uri)
               (.then
                (fn [client]
                  (-> (.getEmitOutput client (str model-uri))
                      (.then
                       (fn [r]
                         (-> r
                             .-outputFiles
                             first
                             .-text)))))))))))

;(let [model-uri (-> (js/monaco.editor.getModels) second .-uri)]
;    (-> (js/monaco.languages.css)
;        (.then
;         (fn [w]
;           (-> (w model-uri)
;               (.then
;                (fn [client]
;                  (-> (.getEmitOutput client (str model-uri))
;                      (.then
;                       (fn [r]
;                         (-> r
;                             .-outputFiles
;                             first
;                             .-text)))))))))))