(ns jaas.jwt
  (:require [clojure.java.io :as io])
  (:import (com.auth0.jwt JWT)
           (java.time Instant)
           (java.util Base64)
           (com.auth0.jwt.algorithms Algorithm)
           (java.security.spec PKCS8EncodedKeySpec)
           (java.security KeyFactory)))

(def expiration-delay 7200)

(defn rsa-private-key [^String pem]
  (->> pem
       (.decode (Base64/getDecoder))
       PKCS8EncodedKeySpec.
       (.generatePrivate (KeyFactory/getInstance "RSA"))))

(defn signed-jwt [options]
  (let [expiration-time (-> (Instant/now) (.plusSeconds expiration-delay) .getEpochSecond)
        not-before (.getEpochSecond (Instant/now))
        pem (rsa-private-key (:private-key options))]
    (-> (JWT/create)
        (.withKeyId (:api-key-id options))
        (.withClaim "iss" "chat")
        (.withClaim "aud" "jitsi")
        (.withClaim "nbf" not-before)
        (.withClaim "exp" expiration-time)
        (.withClaim "room" (:room-name options))
        (.withClaim "sub" (:tenant options))
        (.withClaim "context" {"user"     {"moderator" (:moderator? options)
                                           "id"        (:user-id options)
                                           "name"      (:user-name options)}
                               "features" {"livestreaming" false
                                           "recording"     false
                                           "transcription" false
                                           "outbound-call" false}})
        (.sign (Algorithm/RSA256 nil, pem)))))
