(defproject org.clojars.terazini/clj-bookmarks "0.1.1-SNAPSHOT"
  :description "A client library for for bookmarking services such as
[del.icio.us](http://delicious.com) or [Pinboard](http://pinboard.in).
It provides both anonymous and named access. In the former case the APIs
do not allow as many features as in the latter one."
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/data.zip "0.1.1"]
                 [clj-http "0.6.1"]]
  :min-lein-version "2.0.0"
  :repositories {"stuart" "http://stuartsierra.com/maven2"}
  :profiles {:dev {:plugins [[marginalia "0.7.0"]
                             [lein-midje "2.0.3"]
                             [com.stuartsierra/lazytest "1.2.3"]]
                   :dependencies [[midje "1.4.0"]]}}
  )
