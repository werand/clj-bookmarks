(ns clj-bookmarks.test.util
  (:use [clj-bookmarks.util] :reload
        [midje.sweet])
  (:import [java.util Date]))

(fact
 ;; We initialize the date with a long value, because that is the only way to
 ;; construct a java.util.Date with a fixed value that is not deprecated and
 ;; does not require java.util.Calendar.
 ;; The value is equal to Jan 02 13:09:24 UTC 2011
 (let [d (Date. 1293973764173)
       dstr "2011-01-02T13:09:24Z"]
   (format-date d) => dstr
   (format-date (parse-date dstr)) => dstr))

(fact
  ;; Test for the basic-auth to still work as expected for the standard case
  (basic-auth-request  anything ...url... ...params...)
  =>
  (contains {:status 202})
  (provided
    (clj-http.client/get ...url... anything)
    => {:status 202
        :body "test"}))

(fact
  ;; Test the retry of the last action in case the too-many-requests error is raised
  (basic-auth-request-with-retry anything ...url... ...params... 1 0)
  =>
  (throws Exception "clj-http: status 429")
  (provided
    (clj-http.client/get ...url... anything)
    => {:status 429
        :body "test"}))

(fact
  ;; Test the wait interval doubling for the retries
  (basic-auth-request-with-retry anything ...url... ...params... 1 5)
  =>
  (throws Exception "clj-http: status 429")
  (provided
    (clj-http.client/get ...url... anything) => {:status 429 :body "test"}
    (sleep 1) => nil
    (sleep 2) => nil
    (sleep 4) => nil
    (sleep 8) => nil
    (sleep 16) => nil))
