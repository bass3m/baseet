(ns baseet.db
  (:require [baseet.lifecycle :as life :only (Lifecycle)]))

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

;; should do more, but this is just a placeholder
(defn db-store-init [config]
  (->DbStore config))


;(defn create-db-store [db-params]
  ;(ref db-params))

;; perhaps this should be moved
;(defrecord DocDatabase [store]
  ;(get-db-name [_])
  ;(get-)
  ;)

;(defrecord CouchDocDatabse
  ;DocDatabase
  ;)

;(defn get-db-name []
  ;())
