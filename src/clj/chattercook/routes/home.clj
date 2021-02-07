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
  (:import (java.util UUID)
           (java.time LocalDateTime)))

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

(defn create-event [request]
  (let [event {:creator   (-> request :params :name)
               :date-time (-> request :params :date-time (LocalDateTime/parse))
               :dish      (-> request :params :dish)}
        id (domain/create-event event)]
    (ring.util.response/redirect (str "/event/" id "/") :see-other)))

(defn event [request]
  (let [id (-> request :path-params :id)
        event (db/get-event {:id id})]
    (layout/render request "event-created.html"
                   {:name            (:creator event)
                    :possessive-name (domain/possessive (:creator event)),
                    :event-date      (time/format "dd.MM.yyyy" (:datetime event))
                    :event-time      (time/format "HH:mm" (:datetime event))
                    :dish            (:dish event)
                    :invitation-url  (str "/invitation/" id "/")})))

(defn redirect-to-create [request]
  (ring.util.response/redirect "/create-event/"))

(defn register-guest [request]
  (let [id (-> request :path-params :id)
        event (db/get-event {:id id})]
    (layout/render request "register-guest.html"
                   {:name       (:creator event)
                    :dish       (:dish event)
                    :event-date (time/format "dd.MM.yyyy" (:datetime event))
                    :event-time (time/format "HH:mm" (:datetime event))})))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get redirect-to-create}]
   ["/create-event" {:get redirect-to-create}]              ; deprecated
   ["/create-event/" {:get create-event-form :post create-event}]
   ["/event/:id/" {:get event}]
   ["/invitation/:id/" {:get register-guest}]
   ["/room/" {:get room}]])

