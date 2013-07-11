(ns baseet.models
  (:require [com.ashafa.clutch :as db]
            [suweet.twitter :as suweet :only (get-twitter-lists
                                              make-twitter-creds
                                              get-twitter-list-tweets)]
            [suweet.score :as score :only (score-tweet)]
            [baseet.cfg :as cfg :only (get-twitter-cfg)]
            [clj-time.core :as clj-time]
            [clj-time.coerce :as coerce]))

(defn twitter-creds
  "Simple wrapper around the api call"
  [{:keys [app-consumer-key
           app-consumer-secret
           access-token-key
           access-token-secret]}]
  (suweet/make-twitter-creds app-consumer-key app-consumer-secret
                             access-token-key access-token-secret))

(def my-creds (twitter-creds (cfg/get-twitter-cfg)))

(defn needs-update?
  "Should update out twitter lists once a day. The view returns with a map
   with value of a 2 element array containing the list-id followed by
  last-update-time"
  [tw-lists]
  (some #((complement clj-time/within?)
          (clj-time/interval (-> 1 clj-time/days clj-time/ago) (clj-time/now))
          (coerce/from-string (second (:value %))))
        tw-lists))

(defn build-tw-list-doc
  "Construct the twitter list documents. Add timestamp as well"
  [tw-lists]
  (map (comp #(-> %
                  (assoc :last-update-time (str (clj-time.core/now)))
                  (assoc :schema "tw-list"))
             #(zipmap [:name :list-id :since-id] %)
             (juxt :slug :id (constantly 0)))
       tw-lists))

(defn create-tw-list-design-doc
  "function to programmatically add views for our lists.
  It's more performant to emit the whole doc that later use include_doc
  in the view query. This increases the size the index, but uses less I/O
  resources and results in faster document retrieval."
  [db-name doc-id view-name list-id]
  (db/save-design-document db-name :views (str doc-id)
                           ["javascript"
                            {(str view-name "-tweets")
                             {:map (str "function(doc) {if(doc['schema'] == 'tweet' &&"
                                        " doc['list-id'] == " list-id " )"
                                        "emit(doc['score'],doc['list-id']);}")}}]))

(defn update-twitter-list-views
  "Create view for each of our twitter lists. This should help
  make our queries a lot quicker than getting all our documents"
  [db-name tw-lists]
  (doall
    (map (comp (fn [[db-name doc-id view-name list-id]]
                 (create-tw-list-design-doc db-name doc-id view-name list-id))
               (juxt (constantly db-name) :name :name :list-id)) tw-lists))
  tw-lists)

(defn all-twitter-lists
  "Return a map containing user's twitter lists (list name and id)
  The twitter list db is called tw-lists as well as the document itself."
  [_ ctx]
  (let [db-name (-> ctx :db-params :db-name)
        db-tw-lists-view (-> db-name
                             (db/get-view
                               (-> ctx :db-params :views :twitter-list :view-name)
                               (-> ctx :db-params :views :twitter-list :view-name keyword)))]
    (if (or (empty? db-tw-lists-view)
            (needs-update? db-tw-lists-view))
      (let [latest-tw-lists (build-tw-list-doc
                               (suweet/get-twitter-lists my-creds)) ]
        (->> latest-tw-lists
             (update-twitter-list-views db-name)
             (db/bulk-update db-name))
        ;(as-> latest-tw-lists _ (db/bulk-update db-name _) _)
        (map (juxt :name :list-id) latest-tw-lists))
      (map (comp (juxt #(nth % 3) first) :value) db-tw-lists-view))))

(defn score-tweets
  "Score our tweets and save them in db"
  [list-id tweets]
  (->> (:links tweets)
       (map #(-> %
                 (assoc :schema "tweet")
                 (assoc :list-id list-id)
                 (assoc :score (score/score-tweet
                                 {:tw-score :default}
                                 ((juxt :fav-counts :rt-counts :follow-count) %)))))))

(defn too-old?
  ([tweet-activity-view] (too-old? 3 tweet-activity-view))
  ([days tweet-activity-view]
  ((complement clj-time/within?)
          (clj-time/interval (-> days clj-time/days clj-time/ago) (clj-time/now))
          (coerce/from-string tweet-activity-view))))

(defn mark-old-tweets-for-deletion
  "Get rid of old tweets from db. The tweet view return the timestamp
  in the second item in the value array"
  [db-params]
  (let [db-name (:db-name db-params)
        db-tw-activity-view (-> (:db-name db-params)
                                (db/get-view
                                  (-> db-params :views :tweets :view-name)
                                  (-> db-params :views :tweets :view-name keyword)))]
    (if-let [old-tweets (and (seq db-tw-activity-view)
                             (seq (filter (comp too-old? (comp second :value))
                                          db-tw-activity-view)))]
      (map #(as-> % _
              (assoc _ :_rev (:key _))
              (assoc _ :_id (:id _))
              (assoc _ :_deleted true)) old-tweets))))

(defn update-db-since-id!
  "Side effects a-plenty. First get the list view from db containing the
  latest since-id. Get tweets since the since-id. Update db entry with
  the id obtained from the view query. Finally return the new tweets."
  [db-params list-id]
  (let [db-name (:db-name db-params)
        db-tw-lists-view (-> db-name
                             (db/get-view
                               (-> db-params :views :twitter-list :view-name)
                               (-> db-params :views :twitter-list :view-name keyword)
                               {:key (Integer. list-id)}))]
    (when (seq db-tw-lists-view)
      (let [doc-id (:id (first db-tw-lists-view))
            since-id (-> db-tw-lists-view first :value (nth 2))
            tweets (->> {:list-id list-id :since-id since-id}
                        (suweet/get-twitter-list-tweets my-creds))]
        (db/update-document db-name
                            (db/get-document db-name doc-id)
                            assoc :since-id (:since-id tweets))
        tweets))))

(defn tweet-db-housekeep!
  [db-params list-id]
  (apply conj (mark-old-tweets-for-deletion db-params)
         (->> list-id
              (update-db-since-id! db-params)
              (score-tweets list-id))))

;; make the 10 limit configurable
(defn a-twitter-list
  "Get tweets from a twitter list identied by the list id.
  Option value : unread (only unread tweets) or all. Defaults to all.
  Return top 10 tweets for the list sorted by calculated score"
  [{option :option {id :id list-name :list-name} :list-req} ctx]
  (let [db-name (-> ctx :db-params :db-name)]
    (->> id
         (tweet-db-housekeep! (:db-params ctx))
         (db/bulk-update db-name))
    (-> db-name
        (db/get-view list-name (str list-name "-tweets") {:descending true
                                                          :include_docs true
                                                          :limit 10}))))

;; i guess i could've just created a view which emitted the list-id
;; as key.
  ;(tweet-db-housekeep! (:db-params ctx) id)
            ;(map (juxt :id (comp first (partial into []) :urlers))
                 ;(:links tweets))))
      ;id)))
        ;db-tw-lists-view (-> db-name
        ;(do (->> tweets (score-tweets id) (db/bulk-update db-name))
            ;(map (juxt :id (comp first (partial into []) :urlers))
                 ;(:links tweets))))
      ;id)))

;(defn get-url-summary [tw-id]
  ;)

;(defn mark-tweet-read [tw-id])

;(defn mark-list-read [list-id])

;(defn mark-all-read [])
