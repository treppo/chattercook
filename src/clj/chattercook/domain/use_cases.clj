(ns chattercook.domain.use-cases
  (:require
    [chattercook.db.core :refer [*db*] :as db]
    [chattercook.domain.domain :as domain]
    [jaas.jwt :as jwt]
    [java-time :as time])
  (:import
    (biweekly
      Biweekly
      ICalendar)
    (biweekly.component
      VEvent VAlarm)
    (biweekly.util
      Duration)
    (java.util
      UUID)
    (biweekly.property Trigger)
    (biweekly.parameter Related)))


(defn signed-jwt
  [options config]
  (jwt/signed-jwt
    (merge
      options
      {:api-key-id  (:jaas-api-key-id config)
       :tenant      (:jaas-tenant-name config)
       :user-id     (str (UUID/randomUUID))
       :private-key (:jaas-private-key config)})))


(defn enter-room
  [event-id session config]
  (let [options {:room-name  event-id
                 :moderator? (= :moderator (session event-id))
                 :user-name  (or (:name session) "Guest")}]
    {:jwt                  (signed-jwt options config)
     :room-name            event-id
     :video-service-domain (:video-service-domain config)
     :video-api-url        (:video-api-url config)
     :tenant               (:jaas-tenant-name config)}))


(defn download-ical
  [event-id config]
  (let [event (db/get-event {:id event-id})
        vevent (VEvent.)
        valarm (VAlarm/display (Trigger. (Duration/parse "-PT0H30M"), Related/START) "Bald geht’s los!")
        ical (ICalendar.)]
    (doto vevent
      (.setSummary (domain/event-name event))
      (.setDateStart (time/java-date (domain/date-time event)))
      (.setDuration (Duration/parse "PT1H30M"))
      (.setDescription (str (domain/dish-description event)
                            "\n\n"
                            (domain/ingredients-description event)
                            "\n\n"
                            "Ich freu mich auf dich!"))
      (.setUrl (str (:base-url config) (:invitation-path config) (:id event) "/"))
      (.addAlarm valarm))
    (.addEvent ical vevent)

    {:file (.go (Biweekly/write [ical]))
     :last-modified (:created_at event)}))

