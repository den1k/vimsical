(ns vimsical.frontend.code-editor.core
  (:require [vimsical.frontend.code-editor.config :refer [configure-editor]]
            [vimsical.frontend.code-editor.util :as util]))

(defn require-monaco [cb]
  (js/require.config (clj->js {:paths {"vs" "/js/vs"}}))
  (js/require
   (array "vs/editor/editor.main")
   (fn []
     (configure-editor)
     (util/define-util-fns)
     (cb))))
