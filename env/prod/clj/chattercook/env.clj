(ns chattercook.env
  (:require
    [chattercook.prod-middleware :refer [wrap-force-ssl]]
    [clojure.tools.logging :as log]))


(def defaults
  {:init       (fn []
                 (log/info "\n-=[chattercook started successfully]=-"))
   :stop       (fn []
                 (log/info "\n-=[chattercook has shut down successfully]=-"))
   :middleware wrap-force-ssl})
