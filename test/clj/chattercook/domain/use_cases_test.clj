(ns chattercook.domain.use-cases-test
  (:require [clojure.test :refer :all]
            [chattercook.domain.use-cases :as use-cases]
            [chattercook.db.core :refer [*db*] :as db]
            [jsonista.core :as json]
            [java-time :as time])
  (:import (com.auth0.jwt JWT)
           (java.util Base64)
           (biweekly Biweekly)
           (java.time LocalDateTime)))

(def rsa-key
  "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBALujFVAipbVZtdyJlJyprGZEB6t3LtQ7RLHwDOAAaXLBY2jNiLRL1yDFwty++Tp5cjJW1FukX4K+4nMX/7JUcZZjg0nKRLINtQzs2VGodRvGfVzBEr8qzVjpqfGKL65TXFc4csWavFv1j9qFbDyh3MDk0FmROfn7GDB6ESW50YTPAgMBAAECgYAcUwjzmNdU4d7vFKGnaIGKyngx7QLXrL648Ziv87/3P7Qm7OfW95/Y5aF9uyJaLl0LtknGL7DvRqteXmAN5mti2yJiRMjs2RmCttvyDOiHV3Sx8GTRPU6x5aLtwKU/8HQoGjWyV3yk1kkhRf6827e/mtAZ7LFIySWJYHJ06b6CWQJBAN9X7I0UNjmsGomg9FBlH94d//Oh8DjwuEzM7g9ybiHr49GjQm8IoZp91l7dCTjn1n3qtJg59P9BtMRCQoWxEw0CQQDXEp2QiKfevRtDc8EhCH/G1cpwyCcAJcarSAEiu19bR5pI+tDmbhW6lTKSDTmb0KC95xmux58RxeiTHI+DOLBLAkEAkJOiFVFYKM04AI+ol8JedrKE1Xmmv7VUGKMwF9/DW0IQH8zHXIkB07hR5ObA6Y1qU+hzL3eVGQe3tLQPy+tsoQJANp0xibxJ9JPM0ooYZs0DtXggmhcZYD43ftMgBrPR+RnrodCfL4SQ2/p7Bv94u4p05wRCT1G9oQPClWjRQJ8qlQJBAJJo0QcCBkINdz+cMBXAJitc9SabZMSN903jWF5iZUHBvCilbQIkxx2mAmOMuzj9cg9EinUXM9m+xjTjq4XpS5o=")

(defn payload [jwt]
  (->> jwt
       JWT/decode
       .getPayload
       (.decode (Base64/getDecoder))
       json/read-value))

(deftest room-test
  (testing "using name for video conference jwt"
    (let [config {:jaas-api-key-id  "api key"
                  :jaas-tenant-name "tenant name"
                  :jaas-private-key rsa-key}
          session {:name "Indigo"}

          response (use-cases/enter-room "event-id" session config)

          jwt-payload (->> response :jwt payload)
          user-name (get-in jwt-payload ["context" "user" "name"])]
      (is (= "Indigo" user-name))))

  (testing "make creator moderator"
    (let [config {:jaas-api-key-id  "api key"
                  :jaas-tenant-name "tenant name"
                  :jaas-private-key rsa-key}
          session {:name      "Indigo"
                   "event-id" :moderator}

          response (use-cases/enter-room "event-id" session config)

          jwt-payload (->> response :jwt payload)
          moderator? (get-in jwt-payload ["context" "user" "moderator"])]
      (is (true? moderator?)))))

(defn get-event [^String ical]
  (-> (Biweekly/parse ical)
      .first
      .getEvents
      first))

(defn summary [^String ical]
  (-> (get-event ical)
      .getSummary
      .getValue))

(defn start [^String ical]
  (-> (get-event ical)
      .getDateStart
      .getValue
      .toInstant
      str))

(defn description [^String ical]
  (-> (get-event ical)
      .getDescription
      .getValue))

(defn duration [^String ical]
  (-> (get-event ical)
      .getDuration
      .getValue
      str))

(defn url [^String ical]
  (-> (get-event ical)
      .getUrl
      .getValue))

(def event-description
  (str "Gekocht wird Wiener Schnitzel vegan\n\n"
       "Hier deine Einkaufsliste, vergiss nicht einzukaufen:\n"
       "Kalb\n"
       "Semmelbrösel\n"
       "Eier\n\n"
       "Ich freu mich auf dich!"))

(deftest test-ical-download
  (testing "event as ical file"
    (with-redefs [db/get-event (fn [_] {:id             "event-id"
                                        :offsetdatetime "2021-02-09T19:30-01:00"
                                        :creator        "Max"
                                        :dish           "Wiener Schnitzel vegan"
                                        :ingredients    "Kalb\nSemmelbrösel\nEier"
                                        :created_at     (LocalDateTime/of 2021 02 07 18 35)})]
      (let [config {:base-url        "http://localhost:3000"
                    :invitation-path "/join/"}
            response (use-cases/download-ical "event-id" config)
            ical (:file response)]
        (is (= "Max’ Kochgruppe" (summary ical)))
        (is (= "2021-02-09T20:30:00Z" (start ical)))
        (is (= "PT1H30M" (duration ical)))
        (is (= event-description (description ical)))
        (is (= "http://localhost:3000/join/event-id/" (url ical)))
        (is (= (LocalDateTime/of 2021 02 07 18 35) (:last-modified response)))))))
