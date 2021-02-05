(ns chattercook.routes.home
  (:require
    [jaas.jwt :as jwt]
    [chattercook.layout :as layout]
    [chattercook.domain.domain :as domain]
    [chattercook.config :refer [env]]
    [chattercook.middleware :as middleware]
    [ring.util.response])
  (:import (java.util UUID)))

(defn signed-jwt [options]
  (jwt/signed-jwt
    (merge
      options
      {:api-key-id  (:jaas-api-key-id env)
       :tenant      (:jaas-tenant-name env)
       :private-key (:jaas-private-key env)})))

(defn home-page [request]
  (layout/render request "home.html"))

(defn room [request]
  (let [room-name "MyRoom"
        options {:room-name  room-name
                 :moderator? true
                 :user-id    (str (UUID/randomUUID))
                 :user-name  "Max"}]
    (layout/render request "room.html" {:jwt        (signed-jwt options)
                                        :room-name  room-name
                                        :tenant     (:jaas-tenant-name env)})))

(defn guest-room [request]
  (let [room-name "MyRoom"
        options {:room-name  room-name
                 :moderator? false
                 :user-id    (str (UUID/randomUUID))
                 :user-name  "Stefan"}]
    (layout/render request "room.html" {:jwt        (signed-jwt options)
                                        :room-name  room-name
                                        :tenant     (:jaas-tenant-name env)})))

(defn create-event-form [request]
  (layout/render request "create-event.html"))

(defn event-created [request]
  (layout/render request "event-created.html" {:name (domain/possessive (-> request :params :name))}))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/create-event" {:get create-event-form :post event-created}]
   ["/room" {:get room}]
   ["/guest-room" {:get guest-room}]])

