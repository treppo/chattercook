(ns chattercook.routes.event
  (:require
    [java-time :as time]
    [chattercook.layout :as layout]
    [chattercook.domain.domain :as domain]
    [chattercook.domain.use-cases :as use-cases]
    [chattercook.config :refer [env]]
    [chattercook.clock :refer [*clock*]]
    [chattercook.middleware :as middleware]
    [ring.util.response :as response]
    [chattercook.db.core :refer [*db*] :as db]))

(defn room [request]
  (let [event-id (-> request :path-params :id)
        session (:session request)]
    (layout/render request "room.html"
                   (use-cases/enter-room event-id session env))))

(defn create-event [request]
  (layout/render request "create-event.html"
                 {:suggested-date-time (domain/suggested-event-time)
                  :min-date-time       (domain/earliest-event-time)
                  :max-date-time       (domain/latest-event-time)}))

(defn event-created [request]
  (let [user-event-time (-> request :params :date-time)
        offset (-> request :params :timezone-offset)
        user-name (-> request :params :name)
        event {:creator          user-name
               :date-time        (-> request :params :date-time time/local-date-time)
               :offset-date-time (domain/to-offset-date-time user-event-time offset)
               :dish             (-> request :params :dish)
               :ingredients      (-> request :params :ingredients)}
        id (domain/create-event event)
        session (:session request)]
    (->
      (str "/event/" id "/")
      (response/redirect :see-other)
      (assoc :session (assoc session :name user-name, id :moderator)))))

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
        user-name (-> request :params :name)
        session (:session request)]
    (domain/join id user-name)
    (->
      (str "/event/" id "/")
      (response/redirect :see-other)
      (assoc :session (assoc session :name user-name, id :guest)))))

(defn event [request]
  (let [id (-> request :path-params :id)
        guests (db/get-guests {:event-id id})
        event (domain/get-event id)]
    (time/with-clock
      *clock*
      (layout/render request "event.html"
                     {:creators         (domain/possessive (:creator event))
                      :creator         (:creator event)
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

