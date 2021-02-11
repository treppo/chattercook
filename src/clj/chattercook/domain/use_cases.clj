(ns chattercook.domain.use-cases
  (:require [jaas.jwt :as jwt]
            [chattercook.db.core :refer [*db*] :as db]
            [chattercook.domain.domain :as domain])
  (:import (java.util UUID)))

(defn signed-jwt [options config]
  (jwt/signed-jwt
    (merge
      options
      {:api-key-id  (:jaas-api-key-id config)
       :tenant      (:jaas-tenant-name config)
       :user-id     (str (UUID/randomUUID))
       :private-key (:jaas-private-key config)})))

(defn enter-room [event-id session config]
  (let [event (domain/get-event event-id)
        room-name (str (domain/possessive (:creator event)) " Kochgruppe")
        options {:room-name  room-name
                 :moderator? (= :moderator (session event-id))
                 :user-name  (or (:name session) "Guest")}]

    {:jwt                  (signed-jwt options config)
     :room-name            room-name
     :video-service-domain (:video-service-domain config)
     :video-api-url        (:video-api-url config)
     :tenant               (:jaas-tenant-name config)}))
