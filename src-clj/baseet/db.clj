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

(defmulti couch-views :view-name)

(defmethod couch-views :tw-list-view
  [_]
  (db/view-server-fns :cljs {:twl {:map (fn [doc] (js/emit (aget doc "_id") 3))}}))

(defmethod couch-views :tweet-view
  [_]
  (db/view-server-fns :cljs {:tweets {:map (fn [doc] (js/emit (aget doc "_id") 4))}}))

(defmulti db-store-init :db-type)

(defn make-view [db-name design-doc-name]
  (partial db/save-view db-name design-doc-name))

(defmethod db-store-init :couch
  [db-cfg]
  "CouchDB specific initialization. First check if db is configured,
  if not then create it and initialize the views"
  (let [db-name (:db-name db-cfg)]
    (when (nil? (db/database-info db-name))
      (println "Name:" db-name "db-cfg:" db-cfg)
      (db/get-database db-name)
      ;; this should map over the views
      ((make-view db-name (:design-doc-name db-cfg)) (couch-views db-cfg)))
    (->DbStore db-cfg)))

(defmethod db-store-init :default
  [db-cfg]
  "No init needed, just keep the db cfg that are given"
  (->DbStore db-cfg))
