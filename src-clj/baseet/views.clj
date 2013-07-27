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
      (include-css "/bootstrap/css/bootstrap.min.css")
      (include-css "/font-awesome/css/font-awesome.min.css")
      (include-css "/css/style.css")
      (include-css "/bootstrap/css/bootstrap-responsive.min.css")]
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
    (include-js "http://ajax.googleapis.com/ajax/libs/jquery/1.10.1/jquery.min.js")
    (include-js "/bootstrap/js/bootstrap.min.js")))

(defn render-tweet
  "Tweet template: includes tweet text, url, retweets, etc..
  Modal consumes most of the markup"
  [tweet]
  [:div.tweet
   [:div.row-fluid
    [:div.span9.well.well-small
      [:div.span3
       [:div.check-box
        [:input.tweet-hdr.pull-left
         {:type "checkbox" :id "check-box" :name "check"
          :data-label (str (first (:urlers tweet)))
          :data-id (:_id tweet)}]
       [:label {:for "check-box"}]]
      [:span.tweeter {:style "margin-left:8%;"} (str (first (:urlers tweet)))]]
      [:div.span6.muted {:style (str "font-size:11px;")}
       [:div.span2 [:i.icon-retweet] [:span (:rt-counts tweet)]]
       [:div.span2 [:i.icon-star] [:span (:fav-counts tweet)]]
       [:div.span2 [:i.icon-user] [:span (:follow-count tweet)]]]
      [:div.span3.text-right [:em (time-ago-in-words (:created-at tweet))]]
      [:p (str (first (:text tweet)))]
      (when (seq (:url tweet))
        [:small
         (let [modal-id (:_id tweet)]
           [:div.modal-id
           [:div {:id modal-id
                  :class "modal hide fade" :tabindex "-1"
                  :role "dialog" :aria-labelledby (str modal-id "Label")
                  :aria-hidden "true"}
            [:div.modal-header
             [:button.close {:type "button" :data-dismiss "modal" :aria-hidden "true"} "x"]
             [:h4 {:id (str modal-id "Label")} (str "Retrieving summary for :")]
             [:p (:url tweet)]]
            [:div.modal-body
             [:i.icon-spinner.icon-spin.icon-2x]
             [:p "hardly working.."]]
            [:div.modal-footer
             [:button.btn.btn-small {:data-dismiss "modal" :aria-hidden "true"} "Close"]]]
           [:a {:href (str "#" modal-id) :class "btn"
                :role "button" :data-id modal-id  :data-summarized "false"
                :data-toggle "modal"} "Summarize"]
           [:a {:href (:url tweet) :style (str "margin-left:5px;")}
            (str (:url tweet))]])])]]])

(defn pager-view
  [first-page tw-count tw-per-page]
  (cond-> {:next "li.next" :prev "li.previous"}
    (< tw-count tw-per-page) (assoc :next "li.next.disabled")
    first-page (assoc :prev "li.previous.disabled")))

(defn a-twitter-list
  "Get tweets from a twitter list identied by the list id.
  Option value : unread (only unread tweets) or all. Defaults to all"
  [request]
  (let [tweets (:tweets request)
        total-tw (:total_rows (meta tweets))
        pager (-> (pager-view (:first-page request) (count tweets) 10))]
  (html
    [:div.page-header.span9 {:style "margin:0px; padding-bottom:0px;"}
     [:div.row {:style "margin-left:0px;"}
      [:h3.span9  "Top scoring tweets for "
       [:span.muted [:em (:list-name (first tweets))]]]
      [:small.span3.muted {:style "font-size:103%;margin-top:2.6%;text-align:right;"}
       [:div.btn-group {:style "margin-right:1%;"}
        [:a.btn.btn-small.refresh {:href "#" :data-toggle "tooltip" :data-placement "left"
                           :data-original-title "Refresh tweets"}
         [:i.icon-repeat.refresh]]
        [:a.btn.btn-small.page-read {:href "#" :data-toggle "tooltip" :data-placement "bottom"
                           :data-original-title "Mark Page as Read"}
         [:i.icon-check.page-read]]
        [:a.btn.btn-small.rest-list-read {:href "#" :data-toggle "tooltip" :data-placement "right"
                           :data-original-title "Mark Rest as Read"}
         [:i.icon-check-sign.rest-list-read]]]
       [:span "  Unread  "] [:span.unread-count {:style "color: #0088cc;"} total-tw]]]]
    [:div.span9 {:style "margin-left:0px;"}
     [:ul.pager
      [(-> pager :prev keyword) [:a {:href "#" :data-key (-> tweets first :key)}
                              "&larr; Previous Page"]]
      [(-> pager :next keyword) [:a {:href "#" :data-key (-> tweets last :key)}
                 "Next Page &rarr;"]]]]
    (map (comp render-tweet :value) (take 10 tweets)))))

(defn get-url-summary [summary-sentences]
  (html (for [sentence summary-sentences] [:p sentence])))

;; TODO get rid of if never needed
(defn mark-tweet-read
  "Doesn't do anything right now."
  [_]
  {:update "ok"})

(defn toggle-tweet-state
  [req]
  req)

;; for nor, not much
(defn read-tweet-page
  [_]
  {:update "ok"})

;(defn mark-list-read [list-id])

;(defn mark-all-read [])
