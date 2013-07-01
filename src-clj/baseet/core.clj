(ns baseet.core
  (:require [ring.adapter.jetty :only (run-jetty)]
            [compojure.handler :as h :only (api)] 
            [baseet.routes :as routes :only (app)]))



(defn -main
  ([] (-main 7623))
  ([port] (run-jetty (-> routes/app h/api) 
                     {:port (Integer. port) :join? false})))


