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