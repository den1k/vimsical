(ns vimsical.backend.components.server.interceptors.event
  (:require
   [io.pedestal.interceptor :as interceptor]
   [vimsical.backend.handlers.multi :as events.multi]
   ;;
   ;; * Event handler methods (required for side-effects)
   ;;
   [vimsical.backend.handlers.auth.commands]
   [vimsical.backend.handlers.me.queries]
   [vimsical.backend.handlers.status]
   [vimsical.backend.handlers.vcs.commands]
   [vimsical.backend.handlers.vcs.queries]
   [vimsical.backend.handlers.vims.commands]
   [vimsical.backend.handlers.vims.queries]))

(defn- enter
  "Gets the event from the response body and invoke
  `events.multi/handle-event`. The event is added to the context since we used
  that key for convenience in the request-context multi-spec."
  [context]
  (if-some [event (some-> context :request :body)]
    (events.multi/handle-event (assoc context :event event) event)
    (throw (ex-info "No event found" (select-keys context [:request])))))

(defn- leave
  "Ensure the context has a well-formed response, if the enter interceptor
  doesn't add a body or status we'll set it here."
  [context]
  (update context :response
          (fn [{:keys [status body] :as response}]
            (cond-> response
              (nil? status) (assoc :status 200)
              (nil? body)   (assoc :body [])))))

(def handle-event
  (interceptor/interceptor
   {:name  ::handle-event
    :enter enter
    :leave leave}))
