(ns chattercook.end-to-end-test
  (:require
    [clojure.test :refer [deftest testing is use-fixtures]]
    [ring.mock.request :refer [request]]
    [luminus-migrations.core :as migrations]
    [chattercook.handler :refer [app]]
    [chattercook.config :refer [env]]
    [mount.core :as mount]
    [etaoin.api :refer :all]
    [etaoin.keys :as keys]
    [java-time :as time])
  (:import (java.time ZoneId)))

(def clock (time/mock-clock (time/instant (time/zoned-date-time 2021 2 6 19 30)) (ZoneId/systemDefault)))

(use-fixtures
  :once
  (fn [f]
    (mount/start-with {#'chattercook.clock/*clock* clock})
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

      (go browser (get-element-value browser :share-link))
      (is (true? (has-text? browser {:tag :h1} "Du bist zu meiner Kochgruppe eingeladen, ich freu mich auf Dich!")))
      (is (true? (has-text? browser {:tag :h1} "Lieben Gruß, Max.")))
      (is (true? (has-text? browser :event-info "Gekocht wird Trüffelrisotto")))
      (is (true? (has-text? browser :event-info "Am 07.02.2021 um 19:30 Uhr")))

      (doto browser
        (fill {:tag :input :name :name} "Indigo" keys/tab)
        (click {:tag :button :fn/text "Ja, ich bin dabei"}))

      (is (true? (has-text? browser {:tag :h1} "Max'")))
      (is (true? (has-text? browser {:tag :h1} "Kochgruppe")))
      (is (true? (has-text? browser :event-info "Gekocht wird Trüffelrisotto")))
      (is (true? (has-text? browser :event-info "Am 07.02.2021 um 19:30 Uhr")))
      (is (true? (has-text? browser :ingredients "100g Risottoreis")))
      (is (true? (has-text? browser :ingredients "1 Zwiebel")))
      (is (true? (has-text? browser :ingredients "Trüffelöl")))
      (is (true? (has-text? browser :guests "Indigo")))

      (time/set-clock! clock (time/instant (time/zoned-date-time 2021 2 7 19 20) (ZoneId/systemDefault)))
      (refresh browser)

      (go browser (path (get-element-attr browser :video-link :href)))
      (wait-visible browser :create-event-link)
      (is (true? (has-text? browser {:tag :h1} "Vielen Dank")))
      (is (true? (has-text? browser {:tag :h2} "Erstell' jetzt deine eigene Gruppe")))
      (is (true? (has-text? browser :create-event-link "Los geht's"))))))
