(ns chattercook.core
  (:gen-class)
  (:require
    [chattercook.config :refer [env]]
    [chattercook.handler :as handler]
    [chattercook.nrepl :as nrepl]
    [clojure.tools.cli :refer [parse-opts]]
    [clojure.tools.logging :as log]
    [luminus-migrations.core :as migrations]
    [luminus.http-server :as http]
    [mount.core :as mount]))

;; log uncaught exceptions in threads
(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException
      [_ thread ex]
      (log/error {:what      :uncaught-exception
                  :exception ex
                  :where     (str "Uncaught exception on" (.getName thread))}))))


(def cli-options
  [["-p" "--port PORT" "Port number"
    :parse-fn #(Integer/parseInt %)]])


(mount/defstate ^{:on-reload :noop} http-server
                :start
                (http/start
                  (-> env
                      (update :io-threads #(or % (* 2 (.availableProcessors (Runtime/getRuntime)))))
                      (assoc :handler (handler/app))
                      (update :port #(or (-> env :options :port) %))
                      (select-keys [:handler :host :port])))
                :stop
                (http/stop http-server))


(mount/defstate ^{:on-reload :noop} repl-server
                :start
                (when (env :nrepl-port)
                  (nrepl/start {:bind (env :nrepl-bind)
                                :port (env :nrepl-port)}))
                :stop
                (when repl-server
                  (nrepl/stop repl-server)))


(defn stop-app
  []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))


(defn start-app
  [args]
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))


(defn -main
  [& args]
  (mount/start #'chattercook.config/env)
  (cond
    (nil? (:database-url env))
    (do
      (log/error "Database configuration not found, :database-url environment variable must be set before running")
      (System/exit 1))
    (some #{"init"} args)
    (do
      (migrations/init (select-keys env [:database-url :init-script]))
      (System/exit 0))
    (some #{"create-migration"} args)
    (do
      (migrations/create (second args) (select-keys env [:database-url :init-script]))
      (System/exit 0))
    (some #{"create-code-migration"} args)
    (do
      (migrations/create (second args) (select-keys env [:database-url :init-script]) :edn)
      (System/exit 0))
    (migrations/migration? args)
    (do
      (mount/start #'chattercook.db.core/*db*)
      (migrations/migrate args (select-keys env [:database-url]))
      (System/exit 0))
    :else
    (do
      (mount/start #'chattercook.db.core/*db*)
      (migrations/migrate ["migrate"] (select-keys env [:database-url]))
      (start-app args))))
