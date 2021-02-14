(ns chattercook.handler
  (:require
    [chattercook.middleware :as middleware]
    [chattercook.layout :refer [error-page]]
    [chattercook.routes.event :refer [event-routes]]
    [reitit.ring :as ring]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.webjars :refer [wrap-webjars]]
    [chattercook.env :refer [defaults]]
    [mount.core :as mount]))

(mount/defstate init-app
  :start ((or (:init defaults) (fn [])))
  :stop  ((or (:stop defaults) (fn []))))

(mount/defstate app-routes
  :start
  (ring/ring-handler
    (ring/router
      [(event-routes)])
    (ring/routes
      (ring/create-resource-handler
        {:path "/"})
      (wrap-content-type
        (wrap-webjars (constantly nil)))
      (ring/create-default-handler
        {:not-found
         (constantly (error-page {:status 404, :title "Page not found"}))
         :method-not-allowed
         (constantly (error-page {:status 405, :title "Not allowed"}))
         :not-acceptable
         (constantly (error-page {:status 406, :title "Not acceptable"}))}))))

(defn app []
  (middleware/wrap-base #'app-routes))
