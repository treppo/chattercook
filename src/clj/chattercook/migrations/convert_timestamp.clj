(ns chattercook.migrations.convert-timestamp
  (:require
    [chattercook.db.core :refer [*db*] :as db]))


(defn migrate-up
  [config]
  (doseq [event (db/get-all-events)]
    (db/update-event-offset-date-time {:id (:id event) :offsetdatetime (str (:datetime event))})))
