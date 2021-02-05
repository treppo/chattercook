(ns chattercook.end-to-end-test
  (:require
    [clojure.test :refer [deftest testing is use-fixtures]]
    [ring.mock.request :refer [request]]
    [chattercook.handler :refer [app]]
    [mount.core :as mount]
    [etaoin.api :refer :all]
    [etaoin.keys :as keys]))

(use-fixtures
  :once
  (fn [f]
    (mount/start #'chattercook.config/env
                 #'chattercook.handler/app-routes
                 #'chattercook.core/http-server)
    (f)))

(deftest test-app
  (testing "main route"
    (with-firefox-headless
      {} browser
      (doto browser
        (go "http://localhost:3000/")
        (click {:tag :a :fn/text "Los geht's"})
        (fill {:tag :input :name :name} "Max" keys/tab)
        (click {:tag :button :fn/text "Jetzt erstellen"}))

      (is (true? (has-text? browser :event-name "Max' Kochgruppe"))))))
