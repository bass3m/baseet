(ns baseet.utils
  (:require [clj-time.core :as clj-time :only (interval now in-years in-months
                                                        in-weeks in-days in-hours
                                                        in-minutes in-secs)]
            [clj-time.format :as time-fmt :only (parse formatter)]))

(defn time-ago-in-words
  "Display a more humanly readable time stamp. Twitter uses something similar
  to RFC 822 but not quite, so we need our own time formatter."
  [timestamp]
  (let [created-at (time-fmt/parse
                     (time-fmt/formatter "EEE MMM dd HH:mm:ss Z yyyy")
                     timestamp)
        interval (clj-time/interval created-at (clj-time/now))
        time-interval-map (zipmap [clj-time/in-secs  clj-time/in-minutes
                                   clj-time/in-hours clj-time/in-days
                                   clj-time/in-weeks clj-time/in-months
                                   clj-time/in-years]
                                  ["sec" "minute" "hour" "day"
                                   "week" "month" "year"])]
    (loop [interval-map time-interval-map]
      (if (nil? (first interval-map))
        interval
        (let [time-span ((key (first interval-map)) interval)]
          (if (pos? time-span)
            (let [time-str (val (first interval-map))]
              (clojure.string/join " " [time-span
                                        (cond-> time-str
                                          (> time-span 1) (str "s")) "ago"]))
            (recur (next interval-map))))))))
