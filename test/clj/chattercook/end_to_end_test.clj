(ns chattercook.end-to-end-test
  (:require
    [clojure.test :refer [deftest testing is use-fixtures]]
    [ring.mock.request :refer [request]]
    [luminus-migrations.core :as migrations]
    [chattercook.handler :refer [app]]
    [chattercook.config :refer [env]]
    [mount.core :as mount]
    [etaoin.api :refer :all]
    [etaoin.keys :as keys]))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'chattercook.config/env
                 #'chattercook.handler/app-routes
                 #'chattercook.core/http-server
                 #'chattercook.db.core/*db*)
    (migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(defn path [p] (str "http://localhost:3001" p))

(deftest test-app
  (testing "main route"
    (with-firefox-headless
      {} browser
      (doto browser
        (go (path "/create-event/"))
        (fill {:tag :input :name :name} "Max" keys/tab)
        (fill {:tag :input :name :date-time} "2021-02-07T19:30" keys/tab)
        (fill {:tag :input :name :dish} "Trüffelrisotto" keys/tab)
        (fill {:tag :textarea :name :ingredients} "100g Risottoreis\n1 Zwiebel\nTrüffelöl" keys/tab)
        (click {:tag :button :fn/text "Weiter geht's"}))

      (is (true? (has-text? browser {:tag :h1} "Deine Kochgruppe")))
      (is (true? (has-text? browser :event-info "Am 07.02.2021 um 19:30 Uhr")))
      (is (true? (has-text? browser :event-info "Gekocht wird Trüffelrisotto")))
      (is (=  (path "/invitation/abcdefg/" ) (get-element-value browser :share-link))))))
