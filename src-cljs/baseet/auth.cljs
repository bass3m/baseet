(ns baseet.auth
  (:use-macros [dommy.macros :only [sel sel1]])
  (:require [goog.net.XhrIo :as xhr]
            [goog.events :as events]
            [goog.json :as json]
            [dommy.core :as dom]
            [dommy.attrs :as attrs]))

(defrecord User [email])

(defn user-init []
  (->User (atom {}))) 

(defn handle-login-response
  [user e]
  (.log js.console "Got login response")
  (let [target (.-target e)]
    (if (some #{(.getStatus target)} [200 201 202])
      (let [response (-> target 
                         .getResponseText
                         json/parse)]
        (reset! (:email user) (aget response "email"))
        (.reload window.location)))))

(defn login
  [user assertion]
  (.log js/console @(:email user))
  (xhr/send "/login" (partial handle-login-response user)
            "POST" (json/serialize (clj->js {:assertion assertion}))
            (clj->js  {"Content-Type" "application/json"})))

(defn logout
  [user _]
  (.log js/console "logout")
  (xhr/send "/logout" (fn [e]
                       (.log js/console "logout Event is:")
                        (.log js/console e)
                        (.log js/console user)
                        (reset! (:email user) {}))
                        "POST"))
(defn handle-logout
  [user e]
  (.log js/console "handling logout")
  (.log js/console @(:email user))
  )

(defn auth-init
  "Setup the persona object with required callbacks, listen to sign-in event
  see: https://developer.mozilla.org/en-US/docs/Mozilla/Persona/Quick_Setup"
  []
  (when-let [signin (sel1 :.persona-signin)]
    (let [user (user-init)]
      (.watch (.-id js/navigator)
              (clj->js {:loggedInUser @(:email user) 
                        :onlogin (partial login user)
                        :onlogout (partial logout user)}))
      (dom/listen! signin :click #(.request (.-id js/navigator)))
      user)))


