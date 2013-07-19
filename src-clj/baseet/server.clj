(ns baseet.server
  (:require [ring.adapter.jetty :as jetty :only (run-jetty)]
            [ring.middleware.resource :as ring-resource]
            [ring.middleware.file-info :as ring-file-info]
            [compojure.handler :as handler :only (api site)]
            [baseet.lifecycle :as life :only (Lifecycle)]
            [baseet.routes :as routes :only (app)]))

(defn start
  "Start our web server"
  [svr]
  (try
    (if (.isStopped @svr)
      (do
        (.start @svr)
        (println "ReStarting Web Server"))
      (println "Web Server already started. Stop it first."))
    (catch IllegalArgumentException _
      (when-let [run-server (@svr)]
        (reset! svr run-server)
        (println "Starting Web Server")))))

(defn stop
  [svr]
  (when @svr
    (.stop @svr)
    (println "Stopping Web Server")))

(defrecord WebServer [svr]
  life/Lifecycle
  (start [_] (start svr))
  (stop [_] (stop svr)))

(defn web-server-init [config]
  (->WebServer (atom #(jetty/run-jetty
                        (-> config 
                            routes/app 
                            (ring-resource/wrap-resource "public")
                            (ring-file-info/wrap-file-info) ;; do i need this XXX
                            handler/api)
                        {:port (-> config :server-params :port) :join? false}))))
