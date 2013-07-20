(ns baseet.core
  (:use-macros [dommy.macros :only [sel sel1]])
  (:require [goog.net.XhrIo :as xhr]
            [goog.events :as events]
            [dommy.core :as dom]
            [dommy.template :as t]
            [dommy.attrs :as attrs]))

;; want to memoize the call to the twitter list tweets ?? XXX

(defn log [& more]
  (binding [*print-fn* #(.log js/console %)]
    (apply pr more)))

(defn handle-tw-list-response
  "Receive tweets back from server, replace our current view"
  [event]
  (let [response (.-target event)
        tweets (.getResponseText response)]
    (dom/replace-contents! (sel1 :div.span10) (t/html->nodes tweets))))

(defn get-twitter-list
  "Send a GET request to our server, requesting the tweets for the list that
  the user clicked on."
  [target]
  (let [list-id (attrs/attr target :data-list-id)
        list-name (attrs/attr target :data-list-name)
        url (str "list/" list-id "/" list-name)]
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
