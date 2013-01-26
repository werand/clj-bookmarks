(ns clj-bookmarks.pinboard
  "The `pinboard` namespace provides the implementation of the
  [Pinboard API](http://pinboard.in/howto/#api)."
  (:use [clj-bookmarks core util])
  (:require [clj-http.client :as http]
            [clojure.data.zip :as zf]
            [clojure.data.zip.xml :as zfx]
            [clj-bookmarks.delicious :as del]
            [clojure.string :as string])
  (:import [java.util TimeZone Date]
           [java.text SimpleDateFormat]))

(def pb-base-api-url "https://api.pinboard.in/v1/")

;; ## The Pinboard RSS Feeds
;;
;; The functions here are responsible for getting data out of the
;; Pinboard RSS feeds.

(def pb-base-rss-url "http://feeds.pinboard.in/rss/")

;; ### Parser Functions

(defn rss-date-format
  "Create a `SimpleDateFormat` object for the format used by the
  Pinboard RSS feeds.

  The format object needs to be set to the UTC timezone, otherwise it
  would use the default timezone of the current machine. The dates in
  the RSS do provide a timezone, but it is always UTC and it is
  formatted in a way that `SimpleDateFormat` does not seem to support:
  In the feed there is an appended `+00:00`, but we could only parse
  either `GMT+00:00` or `+0000`."
  []
  (doto 
    (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss")
    (.setTimeZone (TimeZone/getTimeZone "UTC"))))

(defn notes-date-format
  "Create a `SimpleDateFormat` object for the format used by the
  Pinboard Notes feeds.

  The format object needs to be set to the UTC timezone, otherwise it
  would use the default timezone of the current machine. The dates in
  the RSS do provide a timezone, but it is always UTC and it is
  formatted in a way that `SimpleDateFormat` does not seem to support:
  In the feed there is an appended `+00:00`, but we could only parse
  either `GMT+00:00` or `+0000`."
  []
  (doto 
    (SimpleDateFormat. "yyyy-MM-dd HH:mm:ss")
    (.setTimeZone (TimeZone/getTimeZone "UTC"))))

(defn parse-rss-posts
  "Parse a string of RSS data from Pinboard into a list of posts.

  The input is turned into a zipper which we use to extract the data
  from the `item` elements. The fields we need are in sub-elements:

  * `link`: put verbatimly into `url` in the result
  * `dc:subject`: these are the tags which we split into a vector
  * `description`: we call this `desc`
  * `dc:date`: this is parsed into a `Date` object and called `date`."
  [input]
  (zfx/xml-> (str->xmlzip input) :item
            (fn [loc] {:url (zfx/xml1-> loc :link zfx/text)
                       :tags (parse-tags
                              (zfx/xml1-> loc :dc:subject zfx/text))
                       :desc (zfx/xml1-> loc :description zfx/text)
                       :date (parse-date rss-date-format
                              (zfx/xml1-> loc :dc:date zfx/text))})))

;; ### Request Functions

(defn rss-popular
  "Get the currently popular bookmars using the Pinboard RSS feeds.

  We send a GET request to `popular` and parse the response body into
  a seq of bookmarks."
  []
  (-> (http/get (str pb-base-rss-url "popular/"))
      :body
      parse-rss-posts))

(defn rss-recent
  "Get the recent bookmars using the Pinboard RSS feeds.

  We send a GET request to `recent` and parse the response body into
  a seq of bookmarks."
  []
  (-> (http/get (str pb-base-rss-url "recent/"))
      :body
      parse-rss-posts))

(defn rss-bookmarks
  "The `rss-bookmarks` function uses the RSS feeds to perform a query
  for shared bookmarks.

  The parameter map can include `tags` and `user`. `tags` can be
  either a string or a vector of string. The map must not be empty.

  This function sends a GET request with the path `/u:USER/t:TAG/t:TAG`."
  [{:keys [tags user]}]
  (let [tags (if (string? tags) [tags] tags)
        path (string/join "/" (filter (comp not nil?)
                                      (cons  (if user (str "u:" user))
                                             (map #(str "t:" %) tags))))]
    (-> (http/get (str pb-base-rss-url path))
        :body
        parse-rss-posts)))

;; ## The PinboardRSSService Record
;;
;; `PinboardRSSService` implements the `BookmarkService` protocol for the
;; Pinboard RSS feeds. No authentication is required.

(defrecord PinboardRSSService []
  AnonymousBookmarkService
  (bookmarks [srv opts] (rss-bookmarks opts))
  (popular [srv] (rss-popular))
  (recent [srv] (rss-recent)))

(defn parse-pinboard-tags
  "Parse a string of XML data into a seq of tags.

  We turn the data into a zipper and get the text of all leaf nodes."
  [input]
  (zfx/xml-> (str->xmlzip input) zf/children :tag (zfx/attr :tag)))

(defn parse-pinboard-notes
  "Parse a string of XML data into a seq of tags.

  We turn the data into a zipper and get the text of all leaf nodes."
  [input]
  (zfx/xml-> (str->xmlzip input)
             :note
             (fn [loc] {:id (zfx/attr loc :id)
                        :title (zfx/xml1-> loc :title zfx/text)
                        :hash (zfx/xml1-> loc :hash zfx/text)
                        :created (parse-date notes-date-format
                                   (zfx/xml1-> loc :created_at zfx/text))
                        :updated (parse-date notes-date-format
                                   (zfx/xml1-> loc :updated_at zfx/text))
                        :length (zfx/xml1-> loc :length zfx/text)})))

(defn parse-pinboard-note
  "Parse a string of XML data into a seq of tags.

  We turn the data into a zipper and get the text of all leaf nodes.
  The title has no tag, it is embedded within the content of the note."
  [input]
  (zfx/xml-> (str->xmlzip input)
             (fn [loc]
               (let [id (zfx/attr loc :id)
                     hash (zfx/xml1-> loc :hash zfx/text)
                     text (zfx/xml1-> loc :text zfx/text)
                     length (zfx/xml1-> loc :length zfx/text)
                     ititle (zfx/text loc)]
                  {:id id
                   :hash hash
                   :text text
                   :length length
                   ;; Since the title is embedded we have to extract it from the note text
                   :title (.trim (.substring ititle (.length (str hash length)) (- (.length ititle) (.length text))))}))))

(defn parse-tag-result
  "Parse a string of XML data with a response code from the Delicious
  v1 API and either return true or throw an exception.

  The function returns true, when the `code` attribute equals
  `done`. Otherwise a exception is thrown with the code as message."
  [input]
  (let [code (zfx/xml1-> (str->xmlzip input) zfx/text)]
    (if-not (= code "done")
      ; FIXME a better error concept, maybe?
      (throw (Exception. code))
      true)))

(defn tags-get
  "Returns a full list of the user's tags along with the number of times they were used."
  [{:keys [endpoint] :as srv}]
  (-> (basic-auth-request srv (str endpoint "tags/get") {})
    :body
    parse-pinboard-tags))

(defn tag-rename
  "Rename an tag, or fold it in to an existing tag."
  [{:keys [endpoint] :as srv} old new]
  (-> (basic-auth-request srv (str endpoint "tags/rename") {:old old :new new})
    :body
    parse-tag-result))

(defn tag-delete
  "Delete an existing tag."
  [{:keys [endpoint] :as srv} tag]
  (-> (basic-auth-request srv (str endpoint "tags/delete") {:tag tag})
    :body
    ;; Currently pinboard does not return a valid xml string!! Therfore the response is not evaluated.
    ))

(defn notes-list
  "Returns a list of the user's notes."
  [{:keys [endpoint] :as srv}]
  (-> (basic-auth-request srv (str endpoint "notes/list") {})
    :body
    parse-pinboard-notes))

(defn note-get
  "Returns an individual user note. The id property is a 20 character long sha1 hash of the note text."
  [{:keys [endpoint] :as srv} id]
  (-> (basic-auth-request srv (str endpoint (str "notes/" id "/")) {})
    :body
    parse-pinboard-note))

;; The Pinboard API has some more features, they are added here

(extend clj_bookmarks.delicious.DeliciousV1Service
  AuthenticatedExtendedPinboardAPI
  {:get-tags (fn [srv] (tags-get srv))
   :rename-tag (fn [srv old new] (tag-rename srv old new))
   :list-notes (fn [srv] (notes-list srv))
   :get-note (fn [srv id] (note-get srv id))
   :delete-tag (fn [srv tag] (tag-delete srv tag))})

(defn init-pinboard
  "Create a service handle for [Pinboard](http://pinboard.in).

  When called without arguments, the [RSS
  feeds](http://pinboard.in/howto/#rss) are used.

  When you pass a username and password, the
  [API](http://pinboard.in/howto/#api) (which is modeled on the
  Delicious API) is used.

  When you pass the auth-token, the pinboard authentication token
  is used."
  ([] (PinboardRSSService.))
  ([auth-token] (clj_bookmarks.delicious.DeliciousV1Service. pb-base-api-url nil nil auth-token))
  ([user passwd] (del/init-delicious pb-base-api-url user passwd)))
