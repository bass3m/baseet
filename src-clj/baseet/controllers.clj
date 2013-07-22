(ns baseet.controllers
  (:require [suweet.summarize :as summarize :only (summarize)]
            [suweet.links :as links :only (parse-url clean-html)]))


(defn get-url-summary
  "Get url summary for document retrieved from db"
  [db-tweet]
  (if (seq db-tweet)
    (map :sentence (-> (:url db-tweet)
                       links/parse-url
                       links/clean-html
                       summarize/summarize))
    "Something is not quite right. No summary found!"))

(defn mark-tweet-read [tw-id])

(defn mark-list-read [list-id])

(defn mark-all-read [])

