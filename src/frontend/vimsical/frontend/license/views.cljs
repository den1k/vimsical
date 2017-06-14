(ns vimsical.frontend.license.views
  (:require [vimsical.common.util.core :as util]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.user :as user]
            [vimsical.vims :as vims]
            [vimsical.frontend.app.subs :as app.subs]
            [vimsical.frontend.util.dom :refer-macros [e>]]))

(defn mit-license [{:keys [vims]}]
  (let [{:keys [db/uid] ::vims/keys [owner]} vims
        year       "2017"
        url        (str "(https://vimsical.com/vims/" uid ")")
        owner-name (util/space-join (::user/first-name owner)
                                    (::user/last-name owner))]
    (util/space-join "Copyright (c)" year owner-name url "\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\nThe above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\nTHE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.")))

(defn license []
  (let [vims (<sub [::app.subs/vims [:db/uid
                                     ::vims/title
                                     {::vims/owner [:db/uid
                                                    ::user/first-name
                                                    ::user/last-name]}]])]
    [:div.license
     {:on-click (e> (.stopPropagation e))}
     (mit-license {:vims vims})]))
