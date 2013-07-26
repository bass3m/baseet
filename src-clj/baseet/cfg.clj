(ns baseet.cfg
   (:require [suweet.twitter :as suweet :only (make-twitter-creds)]))

(defn get-twitter-cfg
  "Get twitter keys and tokens from config file"
  []
  (-> "twitter-cfg.txt"
      clojure.java.io/resource
      slurp
      read-string))

(defn twitter-creds
  "Simple wrapper around the api call"
  [{:keys [app-consumer-key
           app-consumer-secret
           access-token-key
           access-token-secret]}]
  (suweet/make-twitter-creds app-consumer-key app-consumer-secret
                             access-token-key access-token-secret))

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
(defn twitter-params []
  (-> (get-twitter-cfg) twitter-creds)) 

(defn default-cfg []
  (map->DefaultCfg {:cfg-file "baseet-cfg.txt"
                    :server-params (default-server-params)
                    :db-params (default-db-params)
                    :twitter-params (twitter-params)}))

(defn merge-cfg
  "Read our config file and merge it with the config supplied
  from the user. User configs overwrite config file settings."
  [user-cfg]
  (let [cfg-file (:cfg-file user-cfg)]
    (if (empty? cfg-file)
      user-cfg
      (if (.exists (clojure.java.io/as-file cfg-file))
        (let [existing-cfg (read-string (slurp cfg-file))]
          ;; now we merge the exiting config with what the user specified
          (reduce (fn [acc [k v]]
                     (if (not (contains? acc k))
                       (merge acc (hash-map k v))
                       acc)) user-cfg existing-cfg))
        user-cfg))))
