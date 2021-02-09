(ns chattercook.migrations.convert-timestamp-offset
  (:require [chattercook.db.core :refer [*db*] :as db]
            [java-time :as time])
  (:import (java.time ZoneOffset)))

(defn to-offset-date-time [local-date-time offset]
  (->> offset
       ZoneOffset/ofTotalSeconds
       (.atOffset (time/local-date-time local-date-time))))

(defn migrate-up [config]
  (doseq [event (db/get-all-events)]
    (db/update-event-offset-date-time {:id             (:id event)
                                       :offsetdatetime (str (to-offset-date-time (:datetime event) 3600))})))
