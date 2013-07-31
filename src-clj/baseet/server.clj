(ns baseet.server
  (:require [ring.adapter.jetty :as jetty :only (run-jetty)]
            [ring.middleware.json-params :as ring-params :only (wrap-json-params)]
            [compojure.handler :as handler :only (api)]
            [noir.util.middleware :as noir]
            [baseet.lifecycle :as life :only (Lifecycle)]
            [baseet.routes :as routes :only (app logged-in?)]))

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

(defn wrap-dir-index
  "For now, rewrite to lists by default"
  [handler]
  (fn [req]
    (handler (update-in req [:uri]
                        #(if (= "/" %) "/lists" %)))))

(defn web-server-init [config]
  (->WebServer (atom #(jetty/run-jetty (noir/app-handler
                                         [(-> config
                                              routes/app
                                              ;wrap-dir-index
                                              (ring-params/wrap-json-params)
                                              handler/api)]
                                         :access-rules [{:rules [routes/logged-in?]}])
                                       {:port (-> config :server-params :port)
                                        :join? false}))))
