(ns baseet.deploy
     (:require
       [suweet.twitter :as suweet :only (make-twitter-creds)]))

(defrecord DefaultCfg [server-params db-params])
(defrecord DefaultDbParams [db-type db-name views])
(defrecord DefaultServerParams [port])

(defn get-twitter-cfg
  "Get twitter params from environ variables"
  []
  [(System/getenv "app-consumer-key")
   (System/getenv "app-consumer-secret")
   (System/getenv "access-token-key")
   (System/getenv "acess-token-secret")])

(defn default-server-params []
  (->DefaultServerParams (System/getenv "PORT")))

(defn default-db-params []
  (map->DefaultDbParams {:db-type :couch
                         :db-name (System/getenv "CLOUDANT_URL")
                         :views {:twitter-list {:view-name "tw-list-view" :keys []}
                                 :tweets       {:view-name "by-list"}}}))
(defn default-cfg []
  (map->DefaultCfg {:server-params (default-server-params)
                    :db-params (default-db-params)
                    :twitter-params (->> (get-twitter-cfg)
                                        (apply suweet/make-twitter-creds))}))
