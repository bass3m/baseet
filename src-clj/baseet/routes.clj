(ns baseet.routes
  (:require [compojure.core :refer [GET PUT POST defroutes routes]]
            [compojure.route :as route :only (route)]
            [noir.util.route :refer (restricted)]
            [noir.session :as session :only (get)]
            [baseet.controllers :as c]
            [baseet.models :as m]
            [baseet.views :as v]))

(defn logged-in? [_]
  (session/get :user))

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
;; POST "/tweet-read/<tweet-id>"
;;       => tweet-read : mark tweet as read
;;         <tweet-id> : the tweet id that we already read
;; PUT "/list-done/<list-id>"
;;      => list-done : mark twitter list as read
;;         <list-id> : the list id that we already read
;; PUT "/all-done" => all-done : mark all lists read
(defn app [ctx]
  (routes
    (GET "/" request (c/login request))
    (POST "/login" {params :params} (c/auth ctx params))
    (POST "/logout" request (c/logout request))
    (GET "/lists" request (restricted (-> request
                                          (m/all-twitter-lists ctx)
                                          v/all-twitter-lists)))
    (GET ["/list/:id/:list-name" :id #"\d+" :list-name #"\D+"]
         [& list-req]
         (restricted (as-> list-req _
                           (m/a-twitter-list {:option :all :list-req _} ctx)
                           (v/a-twitter-list _))))
    (GET ["/list-unread/:id/:list-name" :id #"\d+" :list-name #"\D+"]
         [& list-req]
         (restricted (as-> list-req _
                           (m/a-twitter-list {:option :unread :list-req _} ctx)
                           (v/a-twitter-list _))))
    (GET "/summarize/:tw-id" [tw-id] (restricted (-> tw-id
                                                     (m/get-url-summary ctx)
                                                     c/get-url-summary
                                                     v/get-url-summary)))
    (GET ["/list-next-unread/:id/:list-name/:list-key"
          :id #"\d+" :list-name #"\D+" :list-key #"\w+"]
         [& list-req]
         (restricted (as-> list-req _
                           (m/next-in-twitter-list {:option :unread :list-req _} ctx)
                           (v/a-twitter-list _))))
    (GET ["/list-prev-unread/:id/:list-name/:list-key"
          :id #"\d+" :list-name #"\D+" :list-key #"\w+"]
         [& list-req]
         (restricted (as-> list-req _
                           (m/prev-in-twitter-list {:option :unread :list-req _} ctx)
                           (v/a-twitter-list _))))
    (PUT "/read-tweet/:id" [id] (restricted (as-> id _
                                                  (m/mark-tweet-read _ ctx)
                                                  (v/mark-tweet-read _))))
    (PUT ["/mark-many/:list-name/:start/:end"
          :list-name #"\D+" :start #"\w+" :end #"\w+"]
         [& page-params]
         (restricted (as-> page-params _
                           (m/mark-many _ ctx)
                           (v/mark-many _))))
    (PUT ["/toggle-tweet-state/:id" :id #"\w+"] [id]
         (restricted (-> id
                         (m/toggle-tweet-state ctx)
                         v/toggle-tweet-state)))
    (route/resources "/") ;; XXX is this needed ?
    (route/not-found "Sorry, there's nothing here.")))
