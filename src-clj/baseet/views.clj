(ns baseet.views
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-css include-js]]))

(defn render-tw-list
  [tw-list]
  [:div.tw-list 
   [:a  {:href (str "/list/" (second tw-list) "/" (first tw-list)) 
         :data-list-id (second tw-list)} (str (first tw-list))]])

(defn all-twitter-lists
  "Return a map containing user's twitter lists (list name and id)"
  [request]
  (html
    [:head
      [:title "Twitter Lists"]
      [:meta  {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      (include-css "/bootstrap/css/bootstrap.css")
      (include-css "/bootstrap/css/bootstrap-responsive.css")]
    [:body]
    [:div.container-fluid
     [:div.row-fluid
      [:div.span2 [:ul.nav.nav-list.pull-left
                   [:li.nav-header "Twitter Lists" (map render-tw-list request)]]]
      [:div.span10 [:div.well {:style (str "margin-top:5%;" "text-align:center;")}
                    [:h4 "My Twitter Lists"]
                    [:p "Select a list to view the highest scoring tweets"]]]]]
    (include-js "/js/main.js")
    (include-js "/bootstrap/js/bootstrap.min.js")))

(defn render-tweet
  [tweet]
  [:li (str (first (:text tweet)))])

(defn a-twitter-list
  "Get tweets from a twitter list identied by the list id.
  Option value : unread (only unread tweets) or all. Defaults to all"
  [request]
  (html
    [:head
      [:title "Tweet Summaries"]
      [:meta  {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      (include-css "/bootstrap/css/bootstrap.css")
      (include-css "/bootstrap/css/bootstrap-responsive.css")]
     ;(include-css "/css/style.css")
    [:body]
    [:div.container-fluid
     [:div.row-fluid [:div.span3 [:h4 "Tw Lists"]]]
     [:ul.unstyled (map (comp render-tweet :value) request)]]
     (include-js "/js/main.js")
     (include-js "/bootstrap/js/bootstrap.min.js")))  

;(defn get-url-summary [tw-id]
  ;)

;(defn mark-tweet-read [tw-id])

;(defn mark-list-read [list-id])

;(defn mark-all-read [])
