(ns baseet.auth
  (:use-macros [dommy.macros :only [sel1]])
  (:require [goog.net.XhrIo :as xhr]
            [goog.net.cookies :as cookies]
            [goog.json :as json]
            [dommy.core :as dom]))

(defrecord User [email])

(defn user-init []
  (->User (atom {})))

(defn handle-login-response
  [user e]
  (let [target (.-target e)]
    (if (some #{(.getStatus target)} [200 201 202])
      (let [response (-> target
                         .getResponseText
                         json/parse)]
        (reset! (:email user) (aget response "email"))
        (.reload window.location)))))

(defn login
  [user assertion]
  (xhr/send "/login" (partial handle-login-response user)
            "POST" (json/serialize (clj->js {:assertion assertion}))
            (clj->js  {"Content-Type" "application/json"})))

(defn logout
  [user _]
  (xhr/send "/logout" (fn [_] (reset! (:email user) {})) "POST")
  (cookies/remove "ring-session"))

(defn auth-init
  "Setup the persona object with required callbacks, listen to sign-in event
  see: https://developer.mozilla.org/en-US/docs/Mozilla/Persona/Quick_Setup"
  [user]
  (when-let [signin (sel1 :.persona-signin)]
    (.watch (.-id js/navigator)
            (clj->js {:loggedInUser @(:email user)
                      :onlogin (partial login user)
                      :onlogout (partial logout user)}))
    (dom/listen! signin :click #(.request (.-id js/navigator)))))
