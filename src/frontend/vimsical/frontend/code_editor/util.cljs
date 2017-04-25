(ns vimsical.frontend.code-editor.util)

(defn require-monaco [cb]
  (js/require (array "vs/editor/editor.main") cb))
