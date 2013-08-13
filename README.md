#### Baseet
---
##### What is it ?
Baseet is a web app built to utilize my other library **suweet**. It displays my twitter lists and highest scoring tweets from said lists. Also allows me to view a text summary of a url embedded in a tweet and save the url to read-it-later services.

The app is my first attempt at using clojurescript, as well as clojure's web stack. Also my first time playing with couchDB. It's been a blast so far.

I built this app using:

- [Jetty](http://www.eclipse.org/jetty/ ) as the web server.
- [Ring](https://github.com/ring-clojure/ring) Clojure's answer to Python WSGI and Ruby's Rack, provides a nice modular design which can be extended by plugins/middleware.
- [compojure](https://github.com/weavejester/compojure) for routing
- [Hiccup](https://github.com/weavejester/hiccup) for templating.
- [Apache couchDB](http://couchdb.apache.org/) as a database store.
- [Twitter Bootstrap](http://twitter.github.io/bootstrap/) So much easier ! 

#### What is suweet ?
In a nutshell, it's a library that i wrote to help me summarize my insane twitter feed, by assigning each tweet a score based on retweets, favorites etc.. It's able to provide a sorted list of my tweets. Suweet also has the ability to summarize a given url. 

Suweet, uses **Apache Tika**, **Open NLP**, **Snowball Stemmer** and **Luhn's algorithm** to perform the text summarization. 

More on suweet on github: [Suweet](https://github.com/bass3m/suweet "Suweet on gitbug").
#### Todo:
- ~~Add ability to mark tweet as read/unread.~~ Done.
- ~~mark list as read~~ Done.
- ~~refresh tweets~~ Done.
- I want to play around with couchDB's changes API
- ~~Use [friend](https://github.com/cemerick/friend)~~ Used [lib-noir](https://github.com/noir-clojure/lib-noir) instead.