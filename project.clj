(defproject baseet "0.1.0-SNAPSHOT"
  :description "Frontend for ranking and summarizing tweets"
  :url "https://github.com/bass3m/baseet"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src-clj"]
  :min-lein-version "2.0.0"
  :main baseet.core
  :profiles {:dev {:plugins [[com.cemerick/austin "0.1.0"]
                             [lein-ancient "0.4.4"]]}}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-http "0.7.6"]
                 [ring/ring-core "1.2.0"]
                 [ring/ring-jetty-adapter "1.2.0"]
                 [ring-json-params "0.1.3"]
                 [lib-noir "0.6.8"]
                 [org.clojure/clojurescript "0.0-1853"]
                 [com.ashafa/clutch "0.4.0-RC1"]
                 [compojure "1.1.5"]
                 [hiccup "1.0.4"]
                 [prismatic/dommy "0.1.1"]
                 [suweet "0.1.5-SNAPSHOT"]
                 [baseet-twdb "0.1.1-SNAPSHOT"]]
  :plugins  [[lein-cljsbuild "0.3.2"]]
  :cljsbuild  {
    :builds [{
          :source-paths ["src-cljs"]
          :compiler {
            :output-to "resources/public/js/main.js"
            :optimizations :whitespace
            :pretty-print true}}]})
