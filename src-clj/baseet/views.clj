(ns baseet.views
  (:require [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-css include-js]]))

(defn render-tw-list
  [tw-list]
  (let [list-name (first tw-list)
        list-id (second tw-list)]
  [:div.tw-list
   [:a {:href "#tw-list" :data-list-name list-name :data-list-id list-id} (str list-name)]]))

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
    [:script {:type "text/javascript" :language "javascript"} "baseet.core.main()"]
    (include-js "/bootstrap/js/bootstrap.min.js")))

;(defn render-tweet
  ;[tweet]
  ;[:div.tweet
   ;[:div.row
    ;[:blockquote
     ;[:p (str (first (:text tweet)))]
     ;(if (seq (:url tweet))
       ;[:small [:a {:href (:url tweet)} (str (:url tweet))]])]]])

;(defn render-tweet
  ;[tweet]
  ;[:div.tweet [:div.row
   ;[:div.span1 [:img.img-circle {:src (:profile-image-url tweet)}]]
   ;[:div.span7 (str (first (:text tweet)))]]])

;(defn render-tweet
  ;[tweet]
  ;[:tr
   ;[:td [:img.img-circle {:src (:profile-image-url tweet)}]]
   ;[:td (str (first (:urlers tweet)))]
   ;;[:td (str (first (:text tweet)))]
   ;[:td (str (:url tweet))]])


(defn render-tweet
  [tweet]
  [:div.tweet
   [:div.row-fluid
    [:div.span9.well.well-small
      [:div.span9 [:strong (str (first (:urlers tweet)))]]
      [:div.span3 [:em (:last-activity tweet)]]
      [:p (str (first (:text tweet)))]
      (if (seq (:url tweet))
        [:small [:a {:href (:url tweet)} (str (:url tweet))]])]]])


;<ul class="nav nav-list">
  ;<li class="nav-header">
    ;List header
  ;</li>
  ;<li class="active">
    ;<a href="#">Home</a>
  ;</li>
  ;<li>
    ;<a href="#">Library</a>
  ;</li>
  ;...
;</ul>

(defn a-twitter-list
  "Get tweets from a twitter list identied by the list id.
  Option value : unread (only unread tweets) or all. Defaults to all"
  [request]
  (html
    [:h3 "Top scoring tweets for " [:small [:span (:list-name (first request))]]]
    (map (comp render-tweet :value) request)))

; try not using table
    ;[:table.table.table-hover
      ;[:tbody (map (comp render-tweet :value) request)]]

   ;"list-id": "91803828",
   ;"text": [
       ;"RT @swb1192: NSA gif, Pixar-style. http://t.co/Y86E06IExX\n\ncc @aral"
   ;],
   ;"urlers": [
       ;"Phil Hagelberg"
   ;],
   ;"rt-counts": 1048,
   ;"score": 0.2833964305029746,
   ;"fav-counts": 0,
   ;"count": 1,
   ;"last-activity": "2013-07-16T06:09:25Z",
   ;"profile-image-url": "http://a0.twimg.com/profile_images/1808933337/_normal.face",
   ;"url": "http://i.imgur.com/XLQIsnH.gif",
   ;"schema": "tweet",
   ;"id": 355907997828005900,
   ;"follow-count": 3698


;(defn get-url-summary [tw-id]
  ;)

;(defn mark-tweet-read [tw-id])

;(defn mark-list-read [list-id])

;(defn mark-all-read [])
