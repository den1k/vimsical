(ns vimsical.frontend.license.views
  (:require
   [vimsical.common.util.core :as util]
   [vimsical.frontend.app.subs :as app.subs]
   [vimsical.frontend.license.subs :as subs]
   [vimsical.frontend.util.dom :refer-macros [e>]]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.frontend.router.routes :as router.routes]
   [vimsical.user :as user]
   [vimsical.vims :as vims]))


(def vims-query
  [:db/uid
   ::vims/created-at
   ::vims/title
   ::vims/license-addendum
   {::vims/owner
    [:db/uid
     ::user/first-name
     ::user/last-name]}])

(defn timestamp->year
  [timestamp]
  (some-> timestamp js/Date. .getFullYear))

(defn mit-license [{::vims/keys [created-at owner license-addendum] :as vims}]
  (let [year (timestamp->year created-at)
        url  (router.routes/vims-uri vims)]
    [:div
     (util/space-join
      "Copyright (c)" year (user/full-name owner) url
      (when license-addendum (str "\n" license-addendum))
      "\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\nThe above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\nTHE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.")]))

(defn license []
  (let [vims        (<sub [::app.subs/vims [:db/uid]])
        license-str (<sub [::subs/license-string vims])]
    [:div.license
     {:on-click (e> (.stopPropagation e))}
     [:h3 "MIT License"]
     [:div license-str]]))
