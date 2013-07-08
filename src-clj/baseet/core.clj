(ns baseet.core
  (:require [baseet.server :as server]
            [compojure.handler :as handler :only (api site)]
            [baseet.routes :as routes :only (app)]  
            [ring.adapter.jetty :as jetty :only (run-jetty)]
            [baseet.lifecycle :as life :only (Lifecycle)]
            [baseet.db :as db]
            [baseet.cfg :as cfg]))

(defrecord Application [config web-server db-store]
  life/Lifecycle
  (start [_]
    (life/start web-server) ;; what to give it ?
    (life/start db-store)) ;; (mapv start [web-server db-store])
  (stop [_]
    (life/stop db-store)
    (life/stop web-server)))

(defrecord DbStore [db-params]
  life/Lifecycle
  (start [_] (db/start db-params))
  (stop [_] (db/stop db-params)))

;; should do more, but this is just a placeholder
(defn db-store-init [config]
  (->DbStore config))

(defrecord WebServer [svr]
  life/Lifecycle
  (start [_] (server/start svr))
  (stop [_] (server/stop svr)))

(defn web-server-init [config]
  (->WebServer (atom #(jetty/run-jetty
                        (-> config routes/app handler/api)
                        {:port (-> config :server-params :port) :join? false}))))
(defn app
  "Return an instance of the application"
  [config]
  (let [config (cfg/merge-cfg config)
        db (db-store-init (:db-params config))
        web-server (web-server-init config)]
    (->Application config web-server db)))

; provide a cli option later to specify config file
; optionally (def app (atom nil))
(defn -main
  ([] (-main (cfg/default-cfg)))
  ([config]
   (when-not (empty? config)
     (let [system (app config)]
       (life/start system)
       (.. (Runtime/getRuntime) (addShutdownHook (proxy [Thread] []
                                                   (run [] (life/stop system)))))))))
