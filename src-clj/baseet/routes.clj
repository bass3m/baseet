(ns baseet.routes
  (:require [compojure.core :refer [GET PUT POST defroutes routes]]
            [compojure.route :only (route) :as route]
            [baseet.controllers :as c]
            [baseet.models :as m]
            [baseet.views :as v]))

;; Define routes:
;; GET "/lists"    => lists : get our twitter lists
;; GET "/list/<list-id>
;;      => get list-tweets : get all highest scoring tweets from a list
;;         <list-id> : numeric id of twitter list
;; GET "/list-unread/<list-id>
;;      => get list-tweets : get unread highest scoring tweets from a list
;;         <list-id> : numeric id of twitter list
;; GET "/summarize/<tweet-id>"
;;      => summarize : get summary for link.
;;         <tweet-id> : the tweet id containing the url to be summarized
;; PUT "/tweet-done/<tweet-id>"
;;      => tweet-done : mark tweet as read
;;         <tweet-id> : the tweet id that we already read
;; PUT "/list-done/<list-id>"
;;      => list-done : mark twitter list as read
;;         <list-id> : the list id that we already read
;; PUT "/all-done" => all-done : mark all lists read
(defn app [ctx]
  (routes
    (GET "/" request (with-out-str (print request "\n\ncfg:" ctx)))
    (GET "/lists" request (-> request
                              (m/all-twitter-lists ctx)
                              v/all-twitter-lists))
    (GET "/list/:id" [id] (as-> id _
                            (m/a-twitter-list {:option :all :id _} ctx)
                            (v/a-twitter-list _)))
    (GET ["/list/:id/:list-name" :id #"\d+" :list-name #"\D+"]
         [& list-req]
         (as-> list-req _
           (m/a-twitter-list {:option :all :list-req _} ctx)
           (v/a-twitter-list _)))
    (GET ["/list-unread/:id/:list-name" :id #"\d+" :list-name #"\D+"]
         [& list-req]
         (as-> list-req _
           (m/a-twitter-list {:option :unread :list-req _} ctx)
           (v/a-twitter-list _)))
    (GET "/summarize/:tw-id" [tw-id] (-> tw-id
                                         (m/get-url-summary ctx)
                                         c/get-url-summary
                                         v/get-url-summary))
    (route/resources "/") ;; XXX is this needed ?
    (route/not-found "Sorry, there's nothing here.")))
