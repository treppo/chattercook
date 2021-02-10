(ns chattercook.domain.domain
  (:require
    [java-time :as time]
    [clojure.string :as string]
    [chattercook.db.core :refer [*db*] :as db])
  (:import (com.devskiller.friendly_id FriendlyId)
           (java.time ZoneOffset)))

(def early-start-min (time/minutes 30))

(defn possessive [name]
  (if (some (fn [c] (string/ends-with? name c)) ["x" "s" "z" "ß" "ce"])
    (str name "’")
    (str name "s")))

(defn suggested-event-time []
  (let [tomorrow (time/plus (time/local-date) (time/days 1))
        dinnertime (time/format "HH:mm" (time/local-time 18 30))]
    (str tomorrow "T" dinnertime)))

(defn earliest-event-time []
  (str (time/local-date) "T" (time/format "HH:mm" (time/plus (time/local-time) (time/hours 1)))))

(defn latest-event-time []
  (str (time/plus (time/local-date) (time/months 2)) "T" (time/format "HH:mm" (time/local-time))))

(defn create-event [{:keys [creator date-time offset-date-time dish ingredients]}]
  (let [id (FriendlyId/createFriendlyId)]
    (db/create-event!
      {:id             id
       :creator        creator
       :dish           dish
       :ingredients    ingredients
       :offsetdatetime (str offset-date-time)})
    id))

(defn get-event [id]
  (let [db-event (db/get-event {:id id})]
    (-> db-event
        (merge {:date-time (time/offset-date-time (:offsetdatetime db-event))})
        (dissoc :offsetdatetime))))

(defn join [id name]
  (db/add-guest! {:event-id id :name name}))

(defn start-event? [{:keys [date-time]}]
  (time/before? (time/minus date-time early-start-min) (time/offset-date-time)))

(defn to-offset-date-time [local-date-time offset]
  (->> offset
       Integer/parseInt
       ZoneOffset/ofTotalSeconds
       (.atOffset (time/local-date-time local-date-time))))
