(ns vimsical.frontend.license.subs
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.frontend.util.mapgraph :as util.mg]
            [vimsical.frontend.router.routes :as router.routes]
            [vimsical.frontend.vims.subs :as vims.subs]
            [vimsical.user :as user]
            [vimsical.vims :as vims]
            [vimsical.common.util.core :as util]
            [re-frame.interop :as interop]))

(defn timestamp->year
  [timestamp]
  #?(:cljs (some-> timestamp js/Date. .getFullYear)))

(defn mit-license-str [{::vims/keys [created-at owner] :as vims}]
  (let [year (timestamp->year created-at)
        url  (router.routes/vims-uri vims)]
    (util/space-join
     "Copyright (c)"
     year (user/full-name owner) (str "(" url ")")
     "\n\nPermission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the \"Software\"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:\n\nThe above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.\n\nTHE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.")))

(re-frame/reg-sub
 ::license-string
 (fn [[_ vims]] (re-frame/subscribe [::vims.subs/vims vims]))
 (fn [vims] (mit-license-str vims)))

(re-frame/reg-sub
 ::license-string-html-comment
 (fn [[_ vims]] (re-frame/subscribe [::license-string vims]))
 (fn [license-string] (util/space-join "<!--" license-string "-->")))