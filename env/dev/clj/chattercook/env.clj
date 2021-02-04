(ns chattercook.env
  (:require
    [selmer.parser :as parser]
    [clojure.tools.logging :as log]
    [chattercook.dev-middleware :refer [wrap-dev]]))

(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[chattercook started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[chattercook has shut down successfully]=-"))
   :middleware wrap-dev})
