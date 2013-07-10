(ns baseet.cfg)

(defn get-twitter-cfg
  "Get twitter keys and tokens from config file"
  []
  (read-string (slurp "twitter-cfg.txt")))

(defrecord DefaultCfg [cfg-file server-params db-params])
(defrecord DefaultDbParams [db-type db-name views])
(defrecord DefaultServerParams [port])

(defn default-server-params []
  (->DefaultServerParams 7623))

(defn default-db-params []
  (map->DefaultDbParams {:db-type :couch
                         :db-name "tw-db"
                         :views {:twitter-list {:view-name "tw-list-view" :keys []}
                                 :tweets       {:view-name "tweet-activity-view"}}}))

(defn default-cfg []
  (map->DefaultCfg {:cfg-file "baseet-cfg.txt"
                    :server-params (default-server-params)
                    :db-params (default-db-params)}))

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
