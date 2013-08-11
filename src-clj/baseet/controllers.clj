(ns baseet.controllers
  (:require [clj-http.client :as http :only (post)]
            [clojure.data.json :as json :only (read-str write-str)]
            [noir.session :as session :only (put! clear!)]
            [noir.response :as response :only (redirect)]
            [baseet.views :as v]
            [suweet.summarize :as summarize :only (summarize)]
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

(defn verify
  [ctx params]
  (http/post "https://verifier.login.persona.org/verify"
             {:form-params
              {:assertion (-> params first val)
               :audience (-> ctx :server-params :hostname)}}))

(defn auth
  "Authenticate user with persona"
  [ctx params]
  (let [verify-status (-> (verify ctx params)
                          :body
                          (json/read-str :key-fn keyword))]
    (if (= (:status verify-status) "okay")
      (let [user (:email verify-status)]
        (session/put! :user {:email user})
        {:headers  {"Content-Type" "application/json;charset=utf-8"}
         :body (json/write-str {:email user})})
      {:status 401})))

(defn login
  "If we already have a session for the user, redirect to our lists page
  otherwise display the login page"
  [request]
  (if (session/get :user)
    (response/redirect "/lists")
    (v/render-login request)))

(defn logout
  "Logout user, remove cookie"
  [request]
  (session/clear!))

(defn save-tweet
  "Save tweet url"
  [url tw-id ctx]
  (let [body (json/write-str {:url url
                              :tweet_id (str tw-id)
                              :access_token (-> ctx :pocket-params :access-token)
                              :consumer_key (-> ctx :pocket-params :consumer-key)})]
    (when-let [save-resp (try
                           (http/post "https://getpocket.com/v3/add"
                                      {:headers {"Content-Type" "application/json;charset=utf-8"
                                                 "X-Accept" "application/json"}
                                       :body body})
                           (catch Exception e nil))]
      (let [saved-item (-> save-resp :body (json/read-str :key-fn keyword) :item)]
        {:item-id (:item_id saved-item)
         :excerpt (:excerpt saved-item)
         :title (:title saved-item)
         :url (or (:resolved_url saved-item)
                  (:normal_url saved-item))}))))

(defn mark-list-read [list-id])

(defn mark-all-read [])

