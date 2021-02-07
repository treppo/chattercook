(ns chattercook.db.core-test
  (:require
    [chattercook.db.core :refer [*db*] :as db]
    [luminus-migrations.core :as migrations]
    [clojure.test :refer :all]
    [next.jdbc :as jdbc]
    [chattercook.config :refer [env]]
    [mount.core :as mount]))

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
                         (db/create-event! t-conn {:id "abcdefg"} {})

                         (is (= "abcdefg" (:id (db/get-event t-conn {:id "abcdefg"} {}))))))
