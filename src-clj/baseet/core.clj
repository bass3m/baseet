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
    (life/start web-server)
    (life/start db-store)) ;; (mapv start [web-server db-store])
  (stop [_]
    (life/stop db-store)
    (life/stop web-server)))

(defn app
  "Return an instance of the application"
  [config]
  (let [config (cfg/merge-cfg config)
        db (db/db-store-init (:db-params config))
        web-server (server/web-server-init config)]
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
