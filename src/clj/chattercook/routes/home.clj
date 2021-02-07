(ns chattercook.routes.home
  (:require
    [java-time :as time]
    [jaas.jwt :as jwt]
    [chattercook.layout :as layout]
    [chattercook.domain.domain :as domain]
    [chattercook.config :refer [env]]
    [chattercook.middleware :as middleware]
    [ring.util.response]
    [chattercook.db.core :refer [*db*] :as db])
  (:import (java.util UUID)))

(defn signed-jwt [options]
  (jwt/signed-jwt
    (merge
      options
      {:api-key-id  (:jaas-api-key-id env)
       :tenant      (:jaas-tenant-name env)
       :private-key (:jaas-private-key env)})))

(defn room [request]
  (let [room-name "MyRoom"
        options {:room-name  room-name
                 :moderator? false
                 :user-id    (str (UUID/randomUUID))
                 :user-name  (-> request :params :name)}]
    (layout/render request "room.html" {:jwt       (signed-jwt options)
                                        :room-name room-name
                                        :tenant    (:jaas-tenant-name env)})))

(defn create-event-form [request]
  (layout/render request "create-event.html"
                 {:suggested-date-time (domain/suggested-event-time)
                  :min-date-time       (domain/earliest-event-time)
                  :max-date-time       (domain/latest-event-time)}))

(defn event-created [request]
  (let [name (-> request :params :name)
        date-time (-> request :params :date-time)
        dish (-> request :params :dish)]
    (layout/render request "event-created.html"
                   {:name            name
                    :possessive-name (domain/possessive name),
                    :event-date      (time/format "dd.MM.yyyy" (time/local-date-time date-time))
                    :event-time      (time/format "HH:mm" (time/local-date-time date-time))
                    :dish            dish
                    :invitation-url  (str "/invitation/" (domain/create-event) "/")})))

(defn redirect-to-create [request]
  (ring.util.response/redirect "/create-event/"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get redirect-to-create}]
   ["/create-event" {:get redirect-to-create}]              ; deprecated
   ["/create-event/" {:get create-event-form :post event-created}]
   ["/event/" {:get create-event-form :post event-created}]
   ["/room/" {:get room}]])

