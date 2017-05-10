(ns vimsical.frontend.code-editor.core
  (:require [vimsical.frontend.code-editor.config :refer [configure-editor]]))

(defn require-monaco [cb]
  (js/require (array "vs/editor/editor.main")
              (fn []
                (configure-editor)
                (cb))))