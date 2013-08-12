(ns baseet.views
  (:require [baseet.utils :as utils :only (time-ago-in-words)]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [include-css include-js]]))

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
    (include-js "/jquery/1.10.1/jquery.min.js")
    (include-js "/bootstrap/js/bootstrap.min.js")))

(defn render-anchor
  "For a saved tweet, use the excerpt as a tooltip"
  [tweet]
  (let [url (:url tweet)]
    (if (:excerpt tweet)
      [:a.tweet-url {:href url :target "_blank" :style (str "margin-left:5px;")
                     :data-toggle "tooltip" :data-placement "top"
                     :data-original-title (:excerpt tweet)} url]
      [:a.tweet-url {:href url :target "_blank" :style (str "margin-left:5px;")}
         url])))

(defn render-save-button
  [tweet]
  (if (true? (:save tweet))
    [:button.btn.btn-small.save.pull-right.disabled {:href "#"}
     [:i.icon-bookmark {:style "padding-right:3px;color:#4f9fcf;"}] "saved"]
    [:button.btn.btn-small.save.pull-right {:href "#"}
     [:i.icon-bookmark-empty {:style "padding-right:3px;"}] "save"]))

(defn render-url
  "Render the url part of the tweet including the summary modal"
  [tweet]
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
      (render-anchor tweet)
      (render-save-button tweet)])])

(defn render-tweet-row
  [tweet]
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
      [:div.span3.text-right [:em (utils/time-ago-in-words (:created-at tweet))]]
      [:p (str (first (:text tweet)))]
      (when (seq (:url tweet))
        (render-url tweet))]])

(defn render-tweet
  "Tweet template: includes tweet text, url, retweets, etc..
  Modal consumes most of the markup"
  [tweet]
  [:div.tweet (render-tweet-row tweet)])

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
        [:a.btn.btn-small.list-read {:href "#" :data-toggle "tooltip" :data-placement "right"
                           :data-original-title "Mark Rest as Read"}
         [:i.icon-check-sign.list-read]]]
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

;; for now, not much
(defn mark-many
  [_]
  {:update "ok"})

(defn save-tweet
  [tweet]
  (if tweet
    (html (render-tweet-row tweet))
    {:update "ok"}))

(defn render-login
  "Display login page"
  [request]
  (html
    [:head
      [:title "Please login"]
      [:meta  {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1.0"}]
      ;; needed by persona
      [:meta {:http-equiv "X-UA-Compatible" :content "IE=Edge"}]
      (include-css "/bootstrap/css/bootstrap.min.css")
      (include-css "/font-awesome/css/font-awesome.min.css")
      (include-css "/css/style.css")
      (include-css "/persona-css-buttons/persona-buttons.css")
      (include-css "/bootstrap/css/bootstrap-responsive.min.css")]
    [:body {:style "padding-top: 40px; padding-bottom: 40px; background-color: #f5f5f5;"}
     [:div.container
      [:div.persona-signin
       [:h2.persona-signin-header "Please Sign In"]
       [:a.persona-button {:href "#"} [:span "Sign In"]]]]]
    (include-js "https://login.persona.org/include.js")
    (include-js "/js/main.js")
    [:script {:type "text/javascript" :language "javascript"} "baseet.core.main()"]))
