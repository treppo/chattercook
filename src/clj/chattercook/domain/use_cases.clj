(ns chattercook.domain.use-cases
  (:require [jaas.jwt :as jwt])
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
  (let [options {:room-name  event-id
                 :moderator? (= :moderator (session event-id))
                 :user-name  (or (:name session) "Guest")}]
    {:jwt                  (signed-jwt options config)
     :room-name            event-id
     :video-service-domain (:video-service-domain config)
     :video-api-url        (:video-api-url config)
     :tenant               (:jaas-tenant-name config)}))
