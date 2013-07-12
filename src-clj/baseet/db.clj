(ns baseet.db
   (:require [com.ashafa.clutch :as db]
             [baseet.lifecycle :as life :only (Lifecycle)]))

(defn start
  [db-params]
  (when db-params
    ;; should also setup couchdb with views if needed
    (println "Started DB" db-params)))

(defn stop
  [db-params]
  (when db-params
    (println "Stopping DB" db-params)))

(defrecord DbStore [db-params]
  life/Lifecycle
  (start [_] (start db-params))
  (stop [_] (stop db-params)))

(defmulti couch-views keyword)

(defmethod couch-views :tw-list-view
  [_]
  (db/view-server-fns
    :cljs {:tw-list-view
           {:map
            (fn [doc]
              (if (= (aget doc "schema") "tw-list")
                (let [list-id (aget doc "list-id")
                      last-update-time (aget doc "last-update-time")
                      since-id (aget doc "since-id")
                      list-name (aget doc "name")]
                  (js/emit list-id
                           (array list-id last-update-time since-id list-name)))))}}))

(defmethod couch-views :by-list
  [_]
  (db/view-server-fns
    :cljs {:by-list
           {:map
            (fn [doc]
              (if (= (aget doc "schema") "tweet")
                (let [list-id (aget doc "list-id")]
                  (js/emit list-id doc))))}}))

(defmethod couch-views :tweet-activity-view
  [_]
  (db/view-server-fns
    :cljs {:tweet-activity-view
           {:map
            (fn [doc]
              (if (= (aget doc "schema") "tweet")
                (let [list-id (aget doc "list-id")
                      last-activity (aget doc "last-activity")]
                  (js/emit rev
                           (array list-id last-activity)))))}}))

(defmulti db-store-init :db-type)

(defmethod db-store-init :couch
  [db-cfg]
  "CouchDB specific initialization. First check if db is configured,
  if not then create it and initialize the views"
  (clojure.pprint/pprint db-cfg)
  (let [db-name (:db-name db-cfg)]
    (when (nil? (db/database-info db-name))
      (db/get-database db-name)
      (doall  ;; needed due to map's laziness
        (map (comp #((partial db/save-view db-name %) (couch-views %))
                   :view-name val)
             (:views db-cfg))))
    (->DbStore db-cfg)))

(defmethod db-store-init :default
  [db-cfg]
  "No init needed, just keep the db cfg that are given"
  (->DbStore db-cfg))
