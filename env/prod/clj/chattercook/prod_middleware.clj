(ns chattercook.prod-middleware
  (:require
    [ring.util.response :as response]))

(defn wrap-force-ssl
  "Almost like in lib-noir.
   If the request's scheme is not https [and is for 'secure.'], redirect with https.
   Also checks the X-Forwarded-Proto header."
  [app]
  (fn [req]
    (let [headers (:headers req)
          host (headers "host")]
      (if (= "https" (headers "x-forwarded-proto"))
        (app req)
        (response/redirect (str "https://" host (:uri req)) :moved-permanently)))))
