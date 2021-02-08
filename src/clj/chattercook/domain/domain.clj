(ns chattercook.domain.domain
  (:require
    [java-time :as time]
    [clojure.string :as string]
    [chattercook.db.core :refer [*db*] :as db])
  (:import (com.devskiller.friendly_id FriendlyId)))

(def early-start-min (time/minutes 30))

(defn possessive [name]
  (if (some (fn [c] (string/ends-with? name c)) ["x" "s" "z" "ß" "ce"])
    (str name "'")
    (str name "s")))

(defn suggested-event-time []
  (let [tomorrow (time/plus (time/local-date) (time/days 1))
        dinnertime (time/format "HH:mm" (time/local-time 18 30))]
    (str tomorrow "T" dinnertime)))

(defn earliest-event-time []
  (str (time/local-date) "T" (time/format "HH:mm" (time/plus (time/local-time) (time/hours 1)))))

(defn latest-event-time []
  (str (time/plus (time/local-date) (time/months 2)) "T" (time/format "HH:mm" (time/local-time))))

(defn create-event [{:keys [creator date-time dish ingredients]}]
  (let [id (FriendlyId/createFriendlyId)]
    (db/create-event!
      {:id          id
       :creator     creator
       :datetime    date-time
       :dish        dish
       :ingredients ingredients})
    id))

(defn join [id name]
  (db/add-guest! {:event-id id :name name}))

(defn start-event? [{:keys [datetime]}]
  (time/before? (time/minus datetime early-start-min) (time/local-date-time)))
