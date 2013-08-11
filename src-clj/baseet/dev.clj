(ns baseet.dev
  (:require
     [suweet.twitter :as suweet :only (make-twitter-creds)]))

(defn get-cfg
  "Get keys and tokens from config file. cfg-sel is keyword specifying
  which config params we're interested in."
  [cfg-sel]
  (-> "baseet-cfg.txt"
      clojure.java.io/resource
      slurp
      read-string
      cfg-sel))

(defrecord DefaultCfg [cfg-file server-params db-params])
(defrecord DefaultDbParams [db-type db-name views])
(defrecord DefaultServerParams [hostname port])

(defn default-server-params []
  (->DefaultServerParams "http://localhost:7623" 7623))

(defn default-db-params []
  (map->DefaultDbParams {:db-type :couch
                         :db-name "tw-db"
                         :views {:twitter-list {:view-name "tw-list-view" :keys []}
                                 :tweets       {:view-name "by-list"}}}))
(defn default-cfg []
  (map->DefaultCfg {:cfg-file "baseet-cfg.txt"
                    :server-params (default-server-params)
                    :db-params (default-db-params)
                    :twitter-params (->> :twitter-cfg
                                         get-cfg
                                         ((juxt :app-consumer-key
                                                :app-consumer-secret
                                                :access-token-key
                                                :access-token-secret))
                                         (apply suweet/make-twitter-creds))
                    :pocket-params (get-cfg :pocket-cfg)}))
