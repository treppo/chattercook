(ns chattercook.clock
  (:gen-class)
  (:require
    [java-time :as time]
    [mount.core :as mount]))


(mount/defstate ^:dynamic *clock*
                :start (time/system-clock "UTC"))
