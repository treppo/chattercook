(ns chattercook.env
  (:require
    [chattercook.dev-middleware :refer [wrap-dev]]
    [clojure.tools.logging :as log]
    [selmer.parser :as parser]))


(def defaults
  {:init
   (fn []
     (parser/cache-off!)
     (log/info "\n-=[chattercook started successfully using the development profile]=-"))
   :stop
   (fn []
     (log/info "\n-=[chattercook has shut down successfully]=-"))
   :middleware wrap-dev})
