(ns clj-bookmarks.util
  "Common utility functions live in the 'util' namespace."
  (:require [clj-http.client :as http]
	    [clojure.xml :as xml]
	    [clojure.zip :as zip]
	    [clojure.data.zip.xml :as zf]
	    [clojure.data.zip :as zfilter]
	    [clojure.string :as string])
  (:import [java.util TimeZone Date]
	   [java.text SimpleDateFormat]
	   [java.io StringReader]
	   [org.xml.sax InputSource]))

(defn string->input-source
  "Covert a string into a SAX InputSource.

  In order to parse XML data from a string, you need to put it into an
  `InputSource` object. When you pass a string to `clojure.xml/parse`
  it interprets it as a filename."
  [s]
  (InputSource. (StringReader. (.trim s))))

(defn str->xmlzip
  "Turn a string of XML data into a zipper structure."
  [input]
  (-> input
      string->input-source
      xml/parse
      zip/xml-zip))

(defn date-format
  "Create a `SimpleDateFormat` object for the format used by the
  Delicious v1 API.

  The format object needs to be set to the UTC timezone, otherwise it
  would use the default timezone of the current machine."
  []
  (doto (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'")
    (.setTimeZone (TimeZone/getTimeZone "UTC"))))

(defn format-date
  "Convert a `Date` object into a string with the format expected by
  the Delicious v1 API."
  [d]
  (.format (date-format) d))

(defn parse-date
  "Parse a date string in the format used by the Delicious v1 API into
  a `Date` object."
  [date-format input]
  (.parse (date-format) input))

(defn too-many-requests?
  [{:keys [status]}]
  (= 429 status))

(defn sleep [secs]
  (Thread/sleep (* secs 1000)))

(defn basic-auth-request-with-retry
  "Send an HTTP GET request using basic authentication to the given
  URL to which `params` get attached.

  The first argument is a map with the keys `user` and `passwd` used
  for the authentication (usually the service handle records are used
  here)."
  [{:keys [user passwd auth-token] :as srv} url params retry-interval retries]
  #_(println url)
  #_(println auth-token)
  (let [response  (if (nil? auth-token)
                    (http/get url {:query-params params :basic-auth [user passwd] :throw-exceptions false})
                    (http/get url {:query-params (assoc params "auth_token" auth-token) :throw-exceptions false ::debug-body true}))]
    (if (http/success? response)
      response
      (if (and (too-many-requests? response) (> retries 0))
        (do
          (sleep retry-interval)
          (basic-auth-request-with-retry srv url params (* 2 retry-interval) (dec retries)))
        (throw (Exception. (str "clj-http: status " (:status response))))))))

(defn basic-auth-request
  "Send an HTTP GET request using basic authentication to the given
  URL to which `params` get attached.

  The first argument is a map with the keys `user` and `passwd` used
  for the authentication (usually the service handle records are used
  here)."
  [srv url params]
    (basic-auth-request-with-retry srv url params 3 5))

(defn parse-tags
  "Parse a space delimited string of tags into a vector."
  [input]
  (vec (string/split input #"\s")))
