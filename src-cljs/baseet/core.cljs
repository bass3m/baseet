(ns baseet.core
  (:use-macros [dommy.macros :only [sel sel1]])
  (:require [goog.net.XhrIo :as xhr]
            [goog.events :as events]
            [dommy.core :as dom]
            [dommy.template :as t]
            [dommy.attrs :as attrs]))

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

(defn handle-mark-tweet-response
  "Handle response for marking tweet as read"
  [id event]
  (let [target (.-target event)]
        ;summary (.getResponseText target)
        ;modal-body (-> js/document
                       ;(.getElementById id)
                       ;(.getElementsByClassName "modal-body")
                       ;(.item 0))
        ;url-div (-> modal-body
                    ;.-parentNode
                    ;.-nextSibling)]
    (.log js/console target)
    (.log js/console event)
    (log "Got response for mark tweet. id:" id)))

(defn handle-mark-read-click
  "Click handler for a request to summarize a link.
  the data-summarized boolean attribute acts as a cacheing flag."
  [event]
  (let [target (.-target event)
        parent (-> target .-parentNode .-parentNode .-parentNode)
        tweet-id (attrs/attr target :data-id)
        url (str "tweet-read/" tweet-id)]
    (.stopPropagation event)
    (.preventDefault event)
    (xhr/send url (partial handle-mark-tweet-response tweet-id) "POST")))

(defn handle-tw-list-response
  "Receive tweets back from server, replace our current view. Add click
  listeners for summarize buttons"
  [event]
  (let [response (.-target event)
        tweets (.getResponseText response)]
    (dom/replace-contents! (sel1 :div.span10) (t/html->nodes tweets))
    (doall
      (map #(dom/listen! % :click handle-mark-read-click) (sel :.mark-read)))
    (doall
      (map #(.prettyCheckable (js/jQuery %)) (sel :.mark-read))
      (map #(dom/listen! % :click handle-summarize-click) (sel :.modal-id)))))

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

(defn ^:export main []
  "Our main function, called from html file. Google closure doesn't have
  an onready type event, so we have to resort to this."
  (log "Starting app")
  (doall
    (map #(dom/listen! % :click handle-tw-list-click) (sel :.tw-list))))
