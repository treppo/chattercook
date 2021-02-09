(ns chattercook.routes.event
  (:require
    [java-time :as time]
    [jaas.jwt :as jwt]
    [chattercook.layout :as layout]
    [chattercook.domain.domain :as domain]
    [chattercook.config :refer [env]]
    [chattercook.clock :refer [*clock*]]
    [chattercook.middleware :as middleware]
    [ring.util.response :as response]
    [chattercook.db.core :refer [*db*] :as db])
  (:import (java.util UUID)))

(defn signed-jwt [options]
  (jwt/signed-jwt
    (merge
      options
      {:api-key-id  (:jaas-api-key-id env)
       :tenant      (:jaas-tenant-name env)
       :user-id     (str (UUID/randomUUID))
       :private-key (:jaas-private-key env)})))

(defn room [request]
  (let [event-id (-> request :path-params :id)
        options {:room-name  event-id
                 :moderator? false
                 :user-name  "Guest"}]
    (layout/render request "room.html"
                   {:jwt                  (signed-jwt options)
                    :room-name            event-id
                    :video-service-domain (:video-service-domain env)
                    :video-api-url        (:video-api-url env)
                    :tenant               (:jaas-tenant-name env)})))

(defn create-event [request]
  (layout/render request "create-event.html"
                 {:suggested-date-time (domain/suggested-event-time)
                  :min-date-time       (domain/earliest-event-time)
                  :max-date-time       (domain/latest-event-time)}))

(defn event-created [request]
  (let [user-event-time (-> request :params :date-time)
        offset (-> request :params :timezone-offset)
        event {:creator          (-> request :params :name)
               :date-time        (-> request :params :date-time time/local-date-time)
               :offset-date-time (domain/to-offset-date-time user-event-time offset)
               :dish             (-> request :params :dish)
               :ingredients      (-> request :params :ingredients)}
        id (domain/create-event event)]
    (response/redirect (str "/event/" id "/") :see-other)))

(defn join [request]
  (let [id (get-in request [:path-params :id])
        joined? (get-in request [:session id])]
    (if joined?
      (response/redirect (str "/event/" id "/"))

      (let [event (domain/get-event id)]
        (layout/render request "join.html"
                       {:id         (:id event)
                        :name       (:creator event)
                        :dish       (:dish event)
                        :event-date (time/format "dd.MM.yyyy" (:date-time event))
                        :event-time (time/format "HH:mm" (:date-time event))})))))

(defn joined [request]
  (let [id (-> request :params :id)
        name (-> request :params :name)
        session (-> request :session)]
    (domain/join id name)
    (->
      (str "/event/" id "/")
      response/redirect
      (assoc :session (assoc session :user name id true)))))

(defn event [request]
  (let [id (-> request :path-params :id)
        guests (db/get-guests {:event-id id})
        event (domain/get-event id)]
    (time/with-clock
      *clock*
      (layout/render request "event.html"
                     {:creator         (domain/possessive (:creator event))
                      :dish            (:dish event)
                      :event-date      (time/format "dd.MM.yyyy" (:date-time event))
                      :event-time      (time/format "HH:mm" (:date-time event))
                      :event-date-time (:date-time event)
                      :start-event?    (domain/start-event? event)
                      :guests          (map :name guests)
                      :event-id        (:id event)
                      :ingredients     (:ingredients event)
                      :invitation-url  (str "/join/" id "/")}))))

(defn thank-you [request]
  (layout/render request "thank-you.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get (fn [request] (response/redirect "/create-event/"))}]
   ["/create-event" {:get (fn [request] (response/redirect "/create-event/"))}] ; deprecated
   ["/create-event/" {:get create-event :post event-created}]
   ["/join/:id/" {:get join}]
   ["/join/" {:post joined}]
   ["/event/:id/" {:get event}]
   ["/room/:id/" {:get room}]
   ["/thank-you/" {:get thank-you}]])

