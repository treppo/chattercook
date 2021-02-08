(ns chattercook.clock
  (:require [mount.core :as mount]
            [java-time :as time])
  (:gen-class))

(mount/defstate ^:dynamic *clock*
                :start (time/system-clock "UTC"))
