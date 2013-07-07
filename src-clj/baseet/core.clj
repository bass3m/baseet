(ns baseet.core
  (:require [baseet.server :as server]
            [baseet.db :as db]
            [baseet.cfg :as cfg]
            [ring.adapter.jetty :as jetty :only (run-jetty)]
            [compojure.handler :as handler :only (api)]
            [baseet.routes :as r :only (app)]))  

(defprotocol Lifecycle
  "A protocol specifying the lifecycle protocol per S.Siera"
  (start [_] "Starting service")
  (stop [_] "Stopping service"))

(defrecord WebServer [svr]
  Lifecycle
  (start [_] (server/start svr))
  (stop [_] (server/stop svr)))

(defrecord DbStore [db-params]
  Lifecycle
  (start [_] (db/start db-params))
  (stop [_] (db/stop db-params)))

(defrecord Application [config web-server db-store]
  Lifecycle
  (start [_]
    (start web-server) ;; what to give it ?
    (start db-store))
  (stop [_]
    (stop db-store)
    (stop web-server)))

(defn db-store-init [config]
  (->DbStore config))

(defn web-server-init [config]
  (->WebServer (atom #(jetty/run-jetty (-> r/app handler/api)
                                        {:port (:port config) :join? false}))))

(defn app
  "Return an instance of the application"
  [config]
  (let [config (cfg/merge-cfg config)
        db (db-store-init (:db-params config))
        web-server (web-server-init (:server-params config))]
    (->Application config web-server db)))

; provide a cli option later to specify config file
; optionally (def app (atom nil))
(defn -main
  ([] (-main (cfg/default-cfg)))
  ([config]
   (when-not (empty? config)
     (let [system (app config)]
       (start system)
       (.. (Runtime/getRuntime) (addShutdownHook (proxy [Thread] []
                                                   (run [] (stop system)))))))))
