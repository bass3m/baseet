(ns baseet.cfg
   (:require
     [baseet.dev :as dev :only (default-cfg)]
     [baseet.deploy :as deploy :only (default-cfg)]))

(defn default-cfg []
  (if (System/getenv "DEPLOY")
    (deploy/default-cfg)
    (dev/default-cfg)))

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
