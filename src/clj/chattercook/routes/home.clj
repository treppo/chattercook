(ns chattercook.routes.home
  (:require
    [jaas.jwt :as jwt]
    [chattercook.layout :as layout]
    [chattercook.config :refer [env]]
    [clojure.java.io :as io]
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
  (layout/render request "home.html" {:docs (-> "docs/docs.md" io/resource slurp)}))

(defn room [request]
  (let [room-name "MyRoom"
        options {:room-name  room-name
                 :moderator? true
                 :user-id    (str (UUID/randomUUID))}]
    (layout/render request "room.html" {:jwt       (signed-jwt options)
                                        :room-name room-name
                                        :tenant    (:jaas-tenant-name env)})))

(defn guest-room [request]
  (let [room-name "MyRoom"
        options {:room-name  room-name
                 :moderator? false
                 :user-id    (str (UUID/randomUUID))}]
    (layout/render request "room.html" {:jwt       (signed-jwt options)
                                        :room-name room-name
                                        :tenant    (:jaas-tenant-name env)})))

(defn about-page [request]
  (layout/render request "about.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/room" {:get room}]
   ["/guest-room" {:get guest-room}]
   ["/about" {:get about-page}]])

