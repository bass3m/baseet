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
               :audience (str (-> ctx :server-params :hostname) ":"
                              (-> ctx :server-params :port))}}))

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
  [request]
  (if (session/get :user)
    (response/redirect "/lists")
    (v/render-login request)))

(defn logout
  "Logout user, remove cookie"
  []
  (session/clear!))

(defn mark-tweet-read [tw-id])

(defn mark-list-read [list-id])

(defn mark-all-read [])

