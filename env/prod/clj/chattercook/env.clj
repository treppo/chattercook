(ns chattercook.env
  (:require [clojure.tools.logging :as log]
            [chattercook.prod-middleware :refer [wrap-force-ssl]]))

(def defaults
  {:init       (fn []
                 (log/info "\n-=[chattercook started successfully]=-"))
   :stop       (fn []
                 (log/info "\n-=[chattercook has shut down successfully]=-"))
   :middleware wrap-force-ssl})
