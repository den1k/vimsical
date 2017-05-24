(ns vimsical.frontend.quick-search.commands
  (:require [vimsical.frontend.util.content :as util.content]
            [vimsical.frontend.config :as config]
            [vimsical.frontend.code-editor.handlers :as code-editor.handlers]
            [vimsical.frontend.quick-search.handlers :as handlers]))

(def commands
  "Map of command keywords -> map of :title and :dispatch vector. Optionally
  takes a :close? value which defaults to true (see dispatch-result)."
  (let [defaults
        {["new" "new vims"] {:title "New Vims"} ;; todo dispatch

         ["play"]           {:title "► Play"} ;; todo dispatch

         ["pause"]          {:title "❚❚ Pause"} ;; todo dispatch

         ["lorem" "ipsum"]  {:title    "Lorem Ipsum"
                             :dispatch [::code-editor.handlers/paste
                                        (util.content/lorem-ipsum 1)]}}
        dev
        {["clear" "console"] {:title    "Clear JS Console"
                              :dispatch [::handlers/clear-console]}}]
    (cond-> defaults
      config/debug? (merge dev))))
