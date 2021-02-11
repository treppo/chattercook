(ns chattercook.domain.use-cases-test
  (:require [clojure.test :refer :all]
            [chattercook.domain.use-cases :as use-cases]
            [jsonista.core :as json]
            [chattercook.config :refer [env]]
            [mount.core :as mount]
            [luminus-migrations.core :as migrations]
            [chattercook.db.core :refer [*db*] :as db]
            [next.jdbc :as jdbc])
  (:import (com.auth0.jwt JWT)
           (java.util Base64)))

(def rsa-key
  "MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBALujFVAipbVZtdyJlJyprGZEB6t3LtQ7RLHwDOAAaXLBY2jNiLRL1yDFwty++Tp5cjJW1FukX4K+4nMX/7JUcZZjg0nKRLINtQzs2VGodRvGfVzBEr8qzVjpqfGKL65TXFc4csWavFv1j9qFbDyh3MDk0FmROfn7GDB6ESW50YTPAgMBAAECgYAcUwjzmNdU4d7vFKGnaIGKyngx7QLXrL648Ziv87/3P7Qm7OfW95/Y5aF9uyJaLl0LtknGL7DvRqteXmAN5mti2yJiRMjs2RmCttvyDOiHV3Sx8GTRPU6x5aLtwKU/8HQoGjWyV3yk1kkhRf6827e/mtAZ7LFIySWJYHJ06b6CWQJBAN9X7I0UNjmsGomg9FBlH94d//Oh8DjwuEzM7g9ybiHr49GjQm8IoZp91l7dCTjn1n3qtJg59P9BtMRCQoWxEw0CQQDXEp2QiKfevRtDc8EhCH/G1cpwyCcAJcarSAEiu19bR5pI+tDmbhW6lTKSDTmb0KC95xmux58RxeiTHI+DOLBLAkEAkJOiFVFYKM04AI+ol8JedrKE1Xmmv7VUGKMwF9/DW0IQH8zHXIkB07hR5ObA6Y1qU+hzL3eVGQe3tLQPy+tsoQJANp0xibxJ9JPM0ooYZs0DtXggmhcZYD43ftMgBrPR+RnrodCfL4SQ2/p7Bv94u4p05wRCT1G9oQPClWjRQJ8qlQJBAJJo0QcCBkINdz+cMBXAJitc9SabZMSN903jWF5iZUHBvCilbQIkxx2mAmOMuzj9cg9EinUXM9m+xjTjq4XpS5o=")

(defn payload [response]
  (->> response
       :jwt
       JWT/decode
       .getPayload
       (.decode (Base64/getDecoder))
       json/read-value))

(def config {:jaas-api-key-id  "api key"
             :jaas-tenant-name "tenant name"
             :jaas-private-key rsa-key})

(deftest room-test
  (with-redefs [db/get-event (fn [_] {:id             "event-id"
                                      :offsetdatetime "2021-02-09T19:30-01:00"
                                      :creator        "Max"
                                      :dish           "Wiener Schnitzel vegan"
                                      :ingredients    "Kalb\nSemmelbrösel\nEier"})]

    (testing "using name for video conference jwt"
      (let [session {:name "Indigo"}

            response (use-cases/enter-room "event-id" session config)

            user-name (get-in (payload response) ["context" "user" "name"])]

        (is (= "Indigo" user-name))))

    (testing "make creator moderator"
      (let [session {:name      "Indigo"
                     "event-id" :moderator}

            response (use-cases/enter-room "event-id" session config)

            moderator? (get-in (payload response) ["context" "user" "moderator"])]

        (is (true? moderator?))))

    (testing "defaults for name and moderator"
      (let [response (use-cases/enter-room "event-id" {} config)

            moderator? (get-in (payload response) ["context" "user" "moderator"])
            user-name (get-in (payload response) ["context" "user" "name"])]

        (is (false? moderator?))
        (is (= "Guest" user-name))))

    (testing "room name"
      (let [response (use-cases/enter-room "event-id" {} config)
            jwt-room-name (get-in (payload response) ["room"])]

        (is (= "Max’ Kochgruppe" (:room-name response)))
        (is (= "Max’ Kochgruppe"  jwt-room-name))))))
