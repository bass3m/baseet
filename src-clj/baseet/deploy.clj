(ns baseet.deploy
     (:require
       [suweet.twitter :as suweet :only (make-twitter-creds)]))

(defrecord DefaultCfg [server-params db-params])
(defrecord DefaultDbParams [db-type db-name views])
(defrecord DefaultServerParams [hostname port])

(defn get-twitter-cfg
  "Get twitter params from environ variables"
  []
  [(System/getenv "APP_CONSUMER_KEY")
   (System/getenv "APP_CONSUMER_SECRET")
   (System/getenv "ACCESS_TOKEN_KEY")
   (System/getenv "ACCESS_TOKEN_SECRET")])

(defn get-pocket-cfg
  "Get pocket params from environ variables"
  []
  {:consumer-key (System/getenv "POCKET_CONSUMER_KEY")
   :access-token (System/getenv "POCKET_ACCESS_TOKEN")})

(defn default-server-params []
  (->DefaultServerParams
    (System/getenv "PERSONA_HOSTNAME")
    (Integer. (System/getenv "PORT"))))

(defn default-db-params []
  (map->DefaultDbParams {:db-type :couch
                         :db-name (System/getenv "CLOUDANT_URL")
                         :views {:twitter-list {:view-name "tw-list-view" :keys []}
                                 :tweets       {:view-name "by-list"}}}))
(defn default-cfg []
  (map->DefaultCfg {:server-params (default-server-params)
                    :db-params (default-db-params)
                    :twitter-params (->> (get-twitter-cfg)
                                         (apply suweet/make-twitter-creds))
                    :pocket-params (get-pocket-cfg)}))
