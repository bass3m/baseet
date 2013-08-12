(ns baseet.core
  (:use-macros [dommy.macros :only [sel sel1]])
  (:require [goog.net.XhrIo :as xhr]
            [goog.events :as events]
            [dommy.core :as dom]
            [dommy.template :as t]
            [dommy.attrs :as attrs]
            [baseet.auth :as auth]))

(defn log [& more]
  (binding [*print-fn* #(.log js/console %)]
    (apply pr more)))

(defn handle-summary-response
  "Handle the summary response back from server. Replace the spinner with
  the content received, toggle the data-summarized attribute."
  [id event]
  (let [target (.-target event)
        summary (.getResponseText target)
        modal-body (-> js/document
                       (.getElementById id)
                       (.getElementsByClassName "modal-body")
                       (.item 0))
        url-div (-> modal-body
                    .-parentNode
                    .-nextSibling)]
    (dom/set-html! modal-body summary)
    (attrs/set-attr! url-div :data-summarized)))

(defn handle-summarize-click
  "Click handler for a request to summarize a link.
  the data-summarized boolean attribute acts as a cacheing flag."
  [event]
  (let [target (.-target event)
        tweet-id (attrs/attr target :data-id)
        url (str "summarize/" tweet-id)]
    (.stopPropagation event)
    (.preventDefault event)
    (.modal (js/jQuery (str "#" tweet-id)))
    (when (= "false" (attrs/attr target :data-summarized))
      (xhr/send url (partial handle-summary-response tweet-id) "GET"))))

(defn get-unread-count []
  (-> js/document
      (.getElementsByClassName "unread-count")
      (.item 0)))

(defn mark-tweet-unread
  [parent checkbox]
  (let [unread (get-unread-count)]
    (dom/toggle-class! parent "muted")
    (attrs/remove-attr! checkbox :checked)
    ;; TODO look into whether this is a cljs bug ?
    (dom/set-html! unread (inc (js/parseInt (.-innerHTML unread))))))

(defn mark-tweet-read
  [parent checkbox]
  (let [unread (get-unread-count)]
    (dom/add-class! parent "muted")
    (attrs/set-attr! checkbox :checked "checked")
    (dom/set-html! unread (dec (.-innerHTML unread)))))

(defn toggle-tweet-state
  "Handle response for marking tweet as read.
  Update the unread count and mute text accordingly
  It's really a toggle tweet state."
  [parent _]
  (let [unread (-> js/document
                   (.getElementsByClassName "unread-count")
                   (.item 0))
        checkbox (sel1 parent :.tweet-hdr)]
    ;; decrement the total unread count
    ;; toggle the muted class
    (if (attrs/has-class? parent "muted")
      (mark-tweet-unread parent checkbox)
      (mark-tweet-read parent checkbox))))

(defn handle-mark-tweet-state-click
  "Click handler for a request to summarize a link.
  the data-summarized boolean attribute acts as a cacheing flag."
  [event]
  (let [target (.-target event)
        parent (-> target .-parentNode .-parentNode .-parentNode)
        tweet-id (attrs/attr (-> target .-parentNode .-firstChild) :data-id)
        url (str "toggle-tweet-state/" tweet-id)]
    (.stopPropagation event)
    (.preventDefault event)
    (xhr/send url (partial toggle-tweet-state parent) "PUT")))

(defn handle-save-url-response
  "Handle response back from server for saving a url"
  [parent resp]
  (let [target (.-target resp)
        tweet (-> target .getResponseText)]
    (when tweet
      (dom/replace-contents! parent (t/html->nodes tweet))
      (.tooltip (js/jQuery (str "a[href='" (.-innerHTML (.-nextSibling (sel1 parent :a))) "']")))
      (dom/listen! (sel1 parent :.check-box) :click handle-mark-tweet-state-click)
      (dom/listen! (sel1 parent :.modal-id) :click handle-summarize-click)
      (mark-tweet-read parent (sel1 parent :.tweet-hdr)))))

(defn handle-save-url-click
  "Save url by sending request to server"
  [event]
  (let [target (.-target event)
        parent (->> target dom/ancestor-nodes (take 6) (drop 5) first)
        tweet-id (attrs/attr (-> target .-parentNode .-firstChild) :id)
        url (str "save-tweet/" tweet-id)]
    (.stopPropagation event)
    (.preventDefault event)
    (when (seq (re-seq #"save" (.-className target)))
      (xhr/send url (partial handle-save-url-response parent) "PUT"))))

;; forward declarations since we reuse ajax handlers
(declare handle-pager-click get-twitter-list)

(defn handle-page-read-response
  [_]
  (let [tweet-hdrs (sel :.tweet-hdr)]
    (doall
      (map #(when-not (attrs/attr % :checked)
              (toggle-tweet-state (-> % .-parentNode
                                        .-parentNode
                                        .-parentNode) _))
           tweet-hdrs))))

(defn handle-page-read-click
  [event]
  (let [tweet-hdrs (sel :.tweet-hdr)
        tw-list (-> (sel1 :.tw-list.active) .-childNodes (.item 0))
        list-name (-> tw-list (attrs/attr :data-list-name))
        start-key (attrs/attr (first tweet-hdrs) :data-id)
        end-key (attrs/attr (last tweet-hdrs) :data-id)
        url (str "/mark-many/" list-name "/" start-key "/" end-key)]
    (.stopPropagation event)
    (.preventDefault event)
    (xhr/send url handle-page-read-response "PUT")))

(defn handle-refresh-click
  [_]
  (get-twitter-list (.-firstChild (sel1 :.tw-list.active))))

(defn handle-list-read-click
  [event]
  (let [tweet-hdrs (sel :.tweet-hdr)
        tw-list (-> (sel1 :.tw-list.active) .-childNodes (.item 0))
        list-name (-> tw-list (attrs/attr :data-list-name))
        start-key (attrs/attr (first tweet-hdrs) :data-id)
        url (str "/mark-many/" list-name "/" start-key "/0")]
    (.stopPropagation event)
    (.preventDefault event)
    (xhr/send url (comp handle-refresh-click handle-page-read-response) "PUT")))

(defn handle-tw-list-response
  "Receive tweets back from server, replace our current view. Add click
  listeners for summarize buttons"
  [event]
  (let [response (.-target event)
        tweets (.getResponseText response)]
    (dom/replace-contents! (sel1 :div.span10) (t/html->nodes tweets))
    (dom/listen! (sel1 :.previous) :click handle-pager-click)
    (dom/listen! (sel1 :.next) :click handle-pager-click)
    (dom/listen! (sel1 :.refresh) :click handle-refresh-click)
    (dom/listen! (sel1 :.page-read) :click handle-page-read-click)
    (dom/listen! (sel1 :.list-read) :click handle-list-read-click)
    (.tooltip (js/jQuery (str ".btn.refresh")))
    (.tooltip (js/jQuery (str ".btn.page-read")))
    (.tooltip (js/jQuery (str ".btn.list-read")))
    (.tooltip (js/jQuery (str ".tweet-url")))
    (doall
      (map #(dom/listen! % :click handle-mark-tweet-state-click) (sel :.check-box)))
    (doall
      (map #(dom/listen! % :click handle-save-url-click) (sel :.save)))
    (doall
      (map #(dom/listen! % :click handle-summarize-click) (sel :.modal-id)))))

(defn handle-pager-click
  [event]
  (.stopPropagation event)
  (.preventDefault event)
  (when-not (attrs/has-class? (-> event .-target .-parentNode ) "disabled")
    (let [target (.-target event)
          tweet-key (attrs/attr target :data-key)
          tw-list (-> (sel1 :.tw-list.active) .-childNodes (.item 0))
          list-name (-> tw-list (attrs/attr :data-list-name))
          list-id (-> tw-list (attrs/attr :data-list-id))
          url (str list-id "/" list-name "/" tweet-key)]
      (if (attrs/has-class? (.-parentNode target) "next")
        (xhr/send (str "list-next-unread/" url) handle-tw-list-response "GET")
        (xhr/send (str "list-prev-unread/" url) handle-tw-list-response "GET")))))

(defn get-twitter-list
  "Send a GET request to our server, requesting the tweets for the list that
  the user clicked on."
  [target]
  (let [list-id (attrs/attr target :data-list-id)
        list-name (attrs/attr target :data-list-name)
        url (str "list-unread/" list-id "/" list-name)]
    (xhr/send url handle-tw-list-response "GET")))

(defn handle-tw-list-click
  "Click handler for our twitter lists"
  [event]
  (let [target (.-target event)]
    (.stopPropagation event)
    (.preventDefault event)
    (doseq [act (sel :.active)]
      (dom/remove-class! act :active))
    ;; first is our node so our parent is second node
    (-> target dom/ancestor-nodes second (dom/add-class! :active))
    (get-twitter-list target)))

(defn handle-all-twitter-lists
  "Listen for the navlist"
  [user]
  (doall
    (map #(dom/listen! % :click handle-tw-list-click) (sel :.tw-list))))

(defn ^:export main []
  "Our main function, called from html file. Google closure doesn't have
  an onready type event, so we have to resort to this."
  (log "Starting app")
  (let [user (auth/user-init)]
    (auth/auth-init user)
    (handle-all-twitter-lists user)))
