(ns baseet.views
  (:require [clj-time.core :as clj-time :only (interval now in-years in-months
                                               in-weeks in-days in-hours
                                               in-minutes in-secs)]
            [clj-time.format :as time-fmt :only (parse formatter)]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-css include-js]]))

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

(defn render-tw-list
  [tw-list]
  (let [list-name (first tw-list)
        list-id (second tw-list)]
    [:li.tw-list [:a {:href "#tw-list" :data-list-name list-name :data-list-id list-id}
                  (str list-name)]]))

(defn all-twitter-lists
  "Return a map containing user's twitter lists (list name and id)"
  [request]
  (html
    [:head
      [:title "Twitter Lists"]
      [:meta  {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      (include-css "/bootstrap/css/bootstrap.css")
      (include-css "/css/style.css")
      (include-css "/bootstrap/css/bootstrap-responsive.css")]
    [:body]
    [:div.container-fluid
     [:div.row-fluid
      [:div.span2 [:ul.nav.nav-list.pull-left.affix
                   [:li.nav-header "Twitter Lists"]
                   (map render-tw-list request)]]
      [:div.span10 [:div.well {:style (str "margin-top:5%;" "text-align:center;")}
                    [:h4 "My Twitter Lists"]
                    [:p "Select a list to view the highest scoring tweets"]]]]]
    (include-js "/js/main.js")
    ;; need to find a better way than use harcoded name
    [:script {:type "text/javascript" :language "javascript"} "baseet.core.main()"]
    (include-js "/bootstrap/js/bootstrap.min.js")))

(defn render-tweet
  [tweet]
  [:div.tweet
   [:div.row-fluid
    [:div.span9.well.well-small
      [:div.span3 [:strong (str (first (:urlers tweet)))]]
      [:div.span6.muted {:style (str "font-size:11px;")}
       [:div.span2 [:i.icon-retweet] [:span (:rt-counts tweet)]]
       [:div.span2 [:i.icon-star] [:span (:fav-counts tweet)]]
       [:div.span2 [:i.icon-user] [:span (:follow-count tweet)]]]
      [:div.span3.text-right [:em (time-ago-in-words (:created-at tweet))]]
      [:p (str (first (:text tweet)))]
      (if (seq (:url tweet))
        [:small [:a {:href (:url tweet)} (str (:url tweet))]])]]])

(defn a-twitter-list
  "Get tweets from a twitter list identied by the list id.
  Option value : unread (only unread tweets) or all. Defaults to all"
  [request]
  (html
    [:div.page-header.span9
      [:h3 "Top scoring tweets for " [:small [:span (:list-name (first request))]]]]
    (map (comp render-tweet :value) request)))


;(defn get-url-summary [tw-id]
  ;)

;(defn mark-tweet-read [tw-id])

;(defn mark-list-read [list-id])

;(defn mark-all-read [])
