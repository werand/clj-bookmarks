(defproject org.clojars.wjlroe/clj-bookmarks "0.1.1-SNAPSHOT"
  :description "A client library for for bookmarking services such as
[del.icio.us](http://delicious.com) or [Pinboard](http://pinboard.in).
It provides both annonymous and named access. In the former case the APIs
do not allow as many features as in the latter one."
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/data.zip "0.1.1"]
                 [clj-http "0.1.3"]]
  :dev-dependencies [[marginalia "0.3.0"]
                     [lein-midje "2.0.0-SNAPSHOT"]])
