(ns chattercook.db.core-test
  (:require
    [chattercook.db.core :refer [*db*] :as db]
    [luminus-migrations.core :as migrations]
    [clojure.test :refer :all]
    [next.jdbc :as jdbc]
    [chattercook.config :refer [env]]
    [mount.core :as mount])
  (:import (java.time LocalDateTime)))

(use-fixtures
  :once
  (fn [f]
    (mount/start
      #'chattercook.config/env
      #'chattercook.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest test-events
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
                         (db/create-event! t-conn {:id "abcdefg" :datetime (LocalDateTime/parse "2021-02-09T19:30") :creator "Christiane" :dish "Wiener Schnitzel vegan"})

                         (let [db-event (db/get-event t-conn {:id "abcdefg"} {})]
                           (is (= "abcdefg" (:id db-event)))
                           (is (= "Christiane" (:creator db-event)))
                           (is (= "Wiener Schnitzel vegan" (:dish db-event)))
                           (is (= (LocalDateTime/parse "2021-02-09T19:30") (:datetime db-event))))))

(deftest test-guests
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
                         (db/create-event! t-conn {:id "abcdefg" :datetime (LocalDateTime/parse "2021-02-09T19:30") :creator "Christiane" :dish "Wiener Schnitzel vegan"})
                         (db/add-guest! t-conn {:event-id "abcdefg" :name "Indigo"})
                         (db/add-guest! t-conn {:event-id "abcdefg" :name "Jonas"})

                         (let [db-guests (db/get-guests t-conn {:event-id "abcdefg"} {})]
                           (is (= #{{:name "Indigo"} {:name "Jonas"}} (set db-guests))))))
