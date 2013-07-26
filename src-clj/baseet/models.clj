(ns baseet.models
  (:require [com.ashafa.clutch :as db]
            [suweet.twitter :as suweet :only (get-twitter-lists
                                              get-twitter-list-tweets)]
            [suweet.score :as score :only (score-tweet)]
            [baseet.cfg :as cfg :only (get-twitter-cfg)]
            [clj-time.core :as clj-time]
            [clj-time.coerce :as coerce]))

(defn needs-update?
  "Should update out twitter lists once a day. The view returns with a map
   with value containing last-update-time"
  [tw-list-views]
  (some #((complement clj-time/within?)
          (clj-time/interval (-> 1 clj-time/days clj-time/ago) (clj-time/now))
          (coerce/from-string (-> % :value :last-update-time)))
        tw-list-views))

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
                                        " emit(doc['unique-score'],doc);}")}}])
  (db/save-design-document db-name :views (str doc-id "-unread")
                           ["javascript"
                            {(str view-name "-unread-tweets")
                             {:map (str "function(doc) {if(doc['schema'] == 'tweet' &&"
                                        " doc['list-id'] == " list-id
                                        " && doc.unread && doc.unread == true)"
                                        " emit(doc['unique-score'],doc);}")}}]))

(defn create-twitter-list-views
  "Create view for each of our twitter lists. This should help
  make our queries a lot quicker than getting all our documents"
  [db-name tw-lists]
  (doall
    (map (comp (fn [[db-name doc-id view-name list-id]]
                 (create-tw-list-design-doc db-name doc-id view-name list-id))
               (juxt (constantly db-name) :name :name :list-id)) tw-lists))
  tw-lists)

(defn get-from
  "Similar to some. Except that it returns the item matching.
  getter is a function used to get the item"
  [coll getter item]
  (when (seq coll)
    (if (= (getter (first coll)) item)
      (first coll)
      (recur (next coll) getter item))))

(defn update-db-tw-lists!
  "Side-effects of updating db. This function assumes that we have
  existing entries that need to be updated because of age. There is
  also the possibility of the addition of new lists that we haven't been
  tracking before"
  [db-name existing-lists latest-lists]
  (reduce (fn [updates new-list]
            (if-let [existing-list (get-from existing-lists
                                             (comp :list-id :value)
                                             (:list-id new-list))]
              ;; an exiting twitter list, just update the timestamp
              (conj updates (as-> (:value existing-list) _
                              (assoc _ :last-update-time (str (clj-time.core/now)))
                              (assoc _ :_rev (:_rev _))
                              (assoc _ :_id (:_id _))))
              ;; else, this is a new twitter list. create view and entry
              (do
                (create-tw-list-design-doc db-name
                                           (:name new-list)
                                           (:name new-list)
                                           (:list-id new-list))
                (db/put-document db-name new-list)
                updates)))
          [] latest-lists))

(defn all-twitter-lists
  "Return a map containing user's twitter lists (list name and id)
  The twitter list db is called tw-lists as well as the document itself."
  [_ ctx]
  (let [db-name (-> ctx :db-params :db-name)
        db-tw-lists-view (-> db-name
                             (db/get-view
                               (-> ctx :db-params :views :twitter-list :view-name)
                               (-> ctx :db-params :views :twitter-list :view-name keyword)))]
    (cond
      (empty? db-tw-lists-view)
        (let [latest-tw-lists (build-tw-list-doc
                                (suweet/get-twitter-lists (:twitter-params ctx)))]
          (->> latest-tw-lists
               (create-twitter-list-views db-name)
               (db/bulk-update db-name))
          (map (juxt :name :list-id) latest-tw-lists))
      ;; nil is also a logical false in clojure
      (needs-update? db-tw-lists-view)
        (let [latest-tw-lists (build-tw-list-doc
                                (suweet/get-twitter-lists (:twitter-params ctx)))]
          (as-> db-name _
              (update-db-tw-lists! _ db-tw-lists-view latest-tw-lists)
              (db/bulk-update db-name _))
          (map (juxt :name :list-id) latest-tw-lists))
      :else (map (comp (juxt :name :list-id) :value) db-tw-lists-view))))

(defn generate-unique-score
  "Make a unique score string while preserving the correct order.
  This is needed because since we base our tweet scores on favs/retweets
  there is the possibility of getting 0 scores, so we need a tie-breaker.
  Use the tweet-id as the lowest significant 20 digits,
  and for the upper 8 digits, multiply the raw score by 10^8 taking
  care of rounding etc.."
  [raw-score tweet]
  (let [raw-score-factor 100000000
        max-raw-score (- raw-score-factor 1)
        score (java.lang.Math/round (double (* raw-score raw-score-factor)))]
    (format "%08d%020d"
            (if (> score max-raw-score) max-raw-score score) (:id tweet))))

(defn make-tweet-db-doc
  "Score our tweets and save them in db"
  [list-id tweets]
  (->> (:links tweets)
       (map #(let [score (score/score-tweet
                            {:tw-score :default}
                            ((juxt :fav-counts :rt-counts :follow-count) %))]
               (-> %
                   (assoc :schema "tweet")
                   (assoc :unread true)
                   (assoc :save false)
                   (assoc :list-id list-id)
                   (assoc :score score)
                   (assoc :unique-score (generate-unique-score score %)))))))

(defn too-old?
  ([tweet-activity-view] (too-old? 3 tweet-activity-view))
  ([days tweet-activity-view]
  ((complement clj-time/within?)
          (clj-time/interval (-> days clj-time/days clj-time/ago) (clj-time/now))
          (coerce/from-string tweet-activity-view))))

(defn mark-old-tweets-for-deletion
  "Get rid of old tweets from db. Use the by-list view in order to
  get all the tweets from a given list"
  [db-params list-id]
  (let [db-name (:db-name db-params)
        db-tw-activity-view (-> (:db-name db-params)
                                (db/get-view
                                  (-> db-params :views :tweets :view-name)
                                  (-> db-params :views :tweets :view-name keyword)
                                  {:key (str list-id)}))]
    (if-let [old-tweets (and (seq db-tw-activity-view)
                             (seq (filter (comp too-old? (comp :last-activity :value))
                                          db-tw-activity-view)))]
      (map #(as-> % _
              (assoc _ :_rev (-> _ :value :_rev))
              (assoc _ :_id (:id _))
              (assoc _ :_deleted true)) old-tweets))))

(defn update-db-since-id!
  "Side effects a-plenty. First get the list view from db containing the
  latest since-id. Get tweets since the since-id. Update db entry with
  the id obtained from the view query. Finally return the new tweets."
  [{:keys [db-params twitter-params]} list-id]
  (let [db-name (:db-name db-params)
        db-tw-lists-view (-> db-name
                             (db/get-view
                               (-> db-params :views :twitter-list :view-name)
                               (-> db-params :views :twitter-list :view-name keyword)
                               {:key (Integer. list-id)}))]
    (when (seq db-tw-lists-view)
      (let [doc-id (:id (first db-tw-lists-view))
            since-id (:since-id (db/get-document db-name doc-id))
            tweets (->> {:list-id list-id :since-id since-id}
                        (suweet/get-twitter-list-tweets twitter-params))]
        (when (> (:since-id tweets) since-id)
          (db/update-document db-name
                              (db/get-document db-name doc-id)
                              assoc :since-id (:since-id tweets))
          tweets)))))

(defn tweet-db-housekeep!
  [{:keys [db-params] :as ctx} list-id]
  (let [old-tweets (mark-old-tweets-for-deletion db-params list-id)]
    (if-let [new-tweets (some->> list-id
                                 (update-db-since-id! ctx)
                                 (make-tweet-db-doc list-id))]
      (apply conj old-tweets new-tweets)
      old-tweets)))

(defn get-tweets-from-list
  "Get the tweet view for the twitter list"
  [option {:keys [db-params] :as ctx} list-id list-name view-cfg]
  (let [db-name (:db-name db-params)
        doc-id (cond-> list-name
                    (= :unread option) (str "-unread"))]
    (some->> list-id
         (tweet-db-housekeep! ctx)
         (db/bulk-update db-name))
    (as-> db-name _
        (db/get-view _ doc-id (str doc-id "-tweets") view-cfg)
        (with-meta (map #(assoc % :list-name list-name) _) (meta _)))))

;; make the 11 limit configurable
(defn a-twitter-list
  "Get tweets from a twitter list identied by the list id.
  Option value : unread (only unread tweets) or all. Defaults to all.
  Return top 10 tweets for the list sorted by calculated score +
  add an additional tweet for pager."
  [{option :option {id :id list-name :list-name} :list-req} ctx]
  {:first-page true
   :tweets (get-tweets-from-list
             option ctx id list-name
             {:descending true :include_docs true :limit 11})})

(defn prev-in-twitter-list
  "Get previous page of tweets from a twitter list identied by the list id.
  Use list-key as a way to page in db. Needs to be reversed for the correct order
  If the view returned only 1 result (our starting key) that means that we reached the
  beginning, so grab the first entries from view instead."
  [{option :option {id :id
                    list-name :list-name
                    list-key :list-key} :list-req} ctx]
  (let [view-opts {:include_docs true :limit 11 :startkey list-key}
        tweets (get-tweets-from-list
                 option ctx id list-name view-opts)]
    (if (= (count tweets) 1)
      {:first-page true
       :tweets (get-tweets-from-list
                 option ctx id list-name
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
   :tweets (get-tweets-from-list
             option ctx id list-name
             {:descending true :include_docs true :limit 11 :startkey list-key})})

(defn get-url-summary
  "Summarize the requested tweet. The client sends as an id to the db document"
  [tw-id ctx]
  (-> (-> ctx :db-params :db-name)
      (db/get-document tw-id)))

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

(defn read-tweet-page
  "Mark the whole page of tweets to read"
  [page-params ctx]
  (let [db-name (-> ctx :db-params :db-name)
        doc-id (str (:list-name page-params) "-unread")
        start-doc (-> db-name (db/get-document (:start page-params)) :unique-score)
        end-doc (-> db-name (db/get-document (:end page-params)) :unique-score)]
    (as-> db-name _
        (db/get-view _ doc-id (keyword (str doc-id "-tweets"))
                     {:descending true :limit 10
                      :startkey (str start-doc) :endkey (str end-doc)})
        (reduce (fn [updates tweet]
                  (conj updates (-> tweet
                                    :value
                                    (assoc :unread false))))
                [] _)
        (db/bulk-update db-name _))))

;(defn mark-list-read [list-id])

;(defn mark-all-read [])
