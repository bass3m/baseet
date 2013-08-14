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

(defn save-to-service
  "Save url to a read-it-later service"
  [url tw-id ctx tags]
  (let [body (cond-> {:url url
                      :tweet_id (str tw-id)
                      :access_token (-> ctx :pocket-params :access-token)
                      :consumer_key (-> ctx :pocket-params :consumer-key)}
               tags (assoc :tags (clojure.string/join \, tags)))
        json-body (json/write-str body)]
    (when-let [save-resp (try
                           (http/post "https://getpocket.com/v3/add"
                                      {:headers {"Content-Type" "application/json;charset=utf-8"
                                                 "X-Accept" "application/json"}
                                       :body json-body})
                           (catch Exception e nil))]
      (let [saved-item (-> save-resp :body (json/read-str :key-fn keyword) :item)]
        {:item-id (:item_id saved-item)
         :excerpt (:excerpt saved-item)
         :title (:title saved-item)
         :url (or (:resolved_url saved-item)
                  (:normal_url saved-item))}))))

(defn get-text-concepts
  "Get tags for the url, only keep tags with relevance higher than 0.85
  return a vector containg the tags"
  [url ctx]
  (when-let [text-concepts (try
                             (http/post
                               "http://access.alchemyapi.com/calls/url/URLGetRankedConcepts"
                               {:form-params {:apikey (-> ctx :alchemy-params :api-key)
                                              :outputMode (-> ctx :alchemy-params :output-mode)
                                              :url url
                                              :linkedData (-> ctx :alchemy-params :linked-data)}})
                             (catch Exception e nil))]
    (let [concepts (-> text-concepts :body (json/read-str :key-fn keyword) :concepts)]
      (mapv :text (filter (comp (partial < 0.85) #(Double. %) :relevance) concepts)))))

(defn save-tweet
  "Save tweet url"
  [url tw-id ctx]
  (->> ctx
       (get-text-concepts url)
       (save-to-service url tw-id ctx)))
