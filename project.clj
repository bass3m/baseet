(defproject baseet "0.1.0-SNAPSHOT"
  :description "Frontend for ranking and summarizing tweets"
  :url "https://github.com/bass3m/baseet"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :source-paths ["src-clj"]
  :main baseet.core
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [ring/ring-core "1.2.0-RC1"]
                 [ring/ring-jetty-adapter "1.2.0-RC1"]
                 [org.clojure/clojurescript "0.0-1847"]
                 [com.ashafa/clutch "0.4.0-RC1"]
                 [compojure "1.1.5"] 
                 [hiccup "1.0.3"]
                 [prismatic/dommy "0.1.1"]
                 [suweet "0.1.4-SNAPSHOT"]]
  :plugins  [[lein-cljsbuild "0.3.2"]]
  :cljsbuild  {
    :builds [{
          :source-paths ["src-cljs"]
          :compiler {
            :output-to "resources/public/js/main.js"
            :optimizations :whitespace
            :pretty-print true}}]})
