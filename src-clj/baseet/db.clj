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

(defmulti couch-views
  (fn [v]
  (keyword (:view-name v))))

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

(defmethod couch-views :tweet-activity-view
  [_]
  (db/view-server-fns
    :cljs {:tweet-activity-view
           {:map
            (fn [doc]
              (if (= (aget doc "schema") "tweet")
                (js/emit (aget doc "_id")
                         (aget doc "last-activity"))))}}))

(defmulti db-store-init :db-type)

(defn make-view [db-name design-doc-name]
  (partial db/save-view db-name design-doc-name))

(defmethod db-store-init :couch
  [db-cfg]
  "CouchDB specific initialization. First check if db is configured,
  if not then create it and initialize the views"
  (clojure.pprint/pprint db-cfg)
  (let [db-name (:db-name db-cfg)
        design-doc (:design-doc-name db-cfg)]
    (when (nil? (db/database-info db-name))
      (db/get-database db-name)
      (doall  ;; needed due to map's laziness
        (map #((make-view db-name (:view-name %)) (couch-views %))
             (:views db-cfg))))
      ;((make-view db-name (:design-doc-name db-cfg)) (couch-views db-cfg)))
    (->DbStore db-cfg)))

(defmethod db-store-init :default
  [db-cfg]
  "No init needed, just keep the db cfg that are given"
  (->DbStore db-cfg))
