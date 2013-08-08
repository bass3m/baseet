(ns baseet.models
  (:require [com.ashafa.clutch :as db]
            [baseet-twdb.tweetdb :as twdb]))

;; make the 11 limit configurable
(defn a-twitter-list
  "Get tweets from a twitter list identied by the list id.
  Option value : unread (only unread tweets) or all. Defaults to all.
  Return top 10 tweets for the list sorted by calculated score +
  add an additional tweet for pager."
  [{option :option {id :id list-name :list-name} :list-req} ctx]
  {:first-page true
   :tweets (do
             (twdb/db-update-tweets! ctx id)
             (twdb/get-unread-tweets
               option ctx list-name {:descending true
                                     :include_docs true
                                     :limit 11}))})

(defn prev-in-twitter-list
  "Get previous page of tweets from a twitter list identied by the list id.
  Use list-key as a way to page in db. Needs to be reversed for the correct order
  If the view returned only 1 result (our starting key) that means that we reached the
  beginning, so grab the first entries from view instead."
  [{option :option {id :id
                    list-name :list-name
                    list-key :list-key} :list-req} ctx]
  (let [view-opts {:include_docs true :limit 11 :startkey list-key}
        tweets (twdb/get-unread-tweets option ctx list-name view-opts)]
    (if (= (count tweets) 1)
      {:first-page true
       :tweets (twdb/get-unread-tweets
                 option ctx list-name
                 (assoc view-opts :descending true))}
      ;; the reverse requires saving the metadata
      {:first-page false
       :tweets (with-meta (reverse tweets) (meta tweets))})))

(defn next-in-twitter-list
  "Get next page of tweets from a twitter list identied by the list id.
  Use list-key as a way to page in db"
  [{option :option {id :id
                    list-name :list-name
                    list-key :list-key} :list-req} ctx]
  {:first-page false
   :tweets (twdb/get-unread-tweets
             option ctx list-name
             {:descending true :include_docs true :limit 11 :startkey list-key})})

;; TODO rename this
(defn get-url-summary
  "Summarize the requested tweet. The client sends us an id to the db document"
  [tw-id ctx]
  (-> (-> ctx :db-params :db-name)
      (db/get-document tw-id)))

(defn save-tweet
  "Save tweet url"
  [id ctx]
  (let [db-name (-> ctx :db-params :db-name)
        tweet (db/get-document db-name id)]
    (db/update-document db-name
                        (as-> (db/get-document db-name id) _
                              (assoc _ :save true)
                              (assoc _ :_rev (:_rev _))
                              (assoc _ :_id (:_id _))))))

(defn toggle-tweet-state
  "Mark the tweet as read or unread"
  [id ctx]
  (let [db-name (-> ctx :db-params :db-name)
        tweet (db/get-document db-name id)]
    (db/update-document db-name (as-> tweet _
                                      (assoc _ :unread (not (:unread _)))
                                      (assoc _ :_rev (:_rev _))
                                      (assoc _ :_id (:_id _))))))

(defn mark-tweet-read
  "Mark this tweet as read"
  [doc-id ctx]
  (let [db-name (-> ctx :db-params :db-name)]
    (db/update-document db-name
                        (as-> (db/get-document db-name doc-id) _
                              (assoc _ :unread false)
                              (assoc _ :_rev (:_rev _))
                              (assoc _ :_id (:_id _))))))

(defn mark-many
  "Mark the whole page of tweets to read"
  [page-params ctx]
  (let [db-name (-> ctx :db-params :db-name)
        doc-id (str (:list-name page-params) "-unread")
        start-doc (-> db-name
                      (db/get-document (:start page-params))
                      :unique-score)
        end-doc (if (= "0" (:end page-params))
                  (-> db-name
                      (db/get-view doc-id
                                   (keyword (str doc-id "-tweets")) {:limit 1})
                      first
                      :key)
                   (-> db-name
                       (db/get-document (:end page-params))
                       :unique-score))]
    (as-> db-name _
        (db/get-view _ doc-id (keyword (str doc-id "-tweets"))
                     {:descending true
                      :startkey (str start-doc) :endkey (str end-doc)})
        (reduce (fn [updates tweet]
                  (conj updates (-> tweet
                                    :value
                                    (assoc :unread false))))
                [] _)
        (db/bulk-update db-name _))))

(defn all-twitter-lists
  "Return a map containing user's twitter lists (list name and id)
  The twitter list db is called tw-lists as well as the document itself."
  [_ ctx]
  (->> ctx
       twdb/db-get-all-twitter-lists!
       (map (comp (juxt :name :list-id) :value))))
