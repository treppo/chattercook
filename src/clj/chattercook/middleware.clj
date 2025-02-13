(ns chattercook.middleware
  (:require
    [chattercook.config :refer [env]]
    [chattercook.env :refer [defaults]]
    [chattercook.layout :refer [error-page]]
    [chattercook.middleware.formats :as formats]
    [clojure.tools.logging :as log]
    [muuntaja.middleware :refer [wrap-format wrap-params]]
    [ring.adapter.undertow.middleware.session :refer [wrap-session]]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
    [ring.middleware.flash :refer [wrap-flash]]
    [ring.middleware.session.cookie :refer [cookie-store]]))


(defn wrap-internal-error
  [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (error-page {:status  500
                     :title   "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))


(defn wrap-csrf
  [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title  "Invalid anti-forgery token"})}))


(defn wrap-formats
  [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))


(defn wrap-base
  [handler]
  (-> ((:middleware defaults) handler)
      wrap-flash
      (wrap-session {:store        (cookie-store {:key (byte-array (map byte (:session-secret env)))})
                     :cookie-attrs {:http-only true :max-age (:max-cookie-age env)}})
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (dissoc :session)))
      wrap-internal-error))
