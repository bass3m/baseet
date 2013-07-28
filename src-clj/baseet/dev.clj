(ns baseet.dev
  (:require
     [suweet.twitter :as suweet :only (make-twitter-creds)]))

(defn get-twitter-cfg
  "Get twitter keys and tokens from config file"
  []
  (-> "twitter-cfg.txt"
      clojure.java.io/resource
      slurp
      read-string))

(defrecord DefaultCfg [cfg-file server-params db-params])
(defrecord DefaultDbParams [db-type db-name views])
(defrecord DefaultServerParams [port])

(defn default-server-params []
  (->DefaultServerParams 7623))

(defn default-db-params []
  (map->DefaultDbParams {:db-type :couch
                         :db-name "tw-db"
                         :views {:twitter-list {:view-name "tw-list-view" :keys []}
                                 :tweets       {:view-name "by-list"}}}))
(defn default-cfg []
  (map->DefaultCfg {:cfg-file "baseet-cfg.txt"
                    :server-params (default-server-params)
                    :db-params (default-db-params)
                    :twitter-params (->> (get-twitter-cfg)
                                         ((juxt :app-consumer-key
                                                :app-consumer-secret
                                                :access-token-key
                                                :access-token-secret))
                                         (apply suweet/make-twitter-creds))}))
