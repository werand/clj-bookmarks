(ns clj-bookmarks.test.pinboard
  (:use [clj-bookmarks pinboard util] :reload
	[midje.sweet])
  (:import [java.util Date]))

(def popular-xml "
<?xml version='1.0' encoding='UTF-8'?>
 <rdf:RDF xmlns='http://purl.org/rss/1.0/' xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' xmlns:content='http://purl.org/rss/1.0/modules/content/' xmlns:taxo='http://purl.org/rss/1.0/modules/taxonomy/' xmlns:dc='http://purl.org/dc/elements/1.1/' xmlns:cc='http://web.resource.org/cc/' xmlns:syn='http://purl.org/rss/1.0/modules/syndication/' xmlns:admin='http://webns.net/mvcb/'>
  <channel rdf:about='http://pinboard.in'>
    <title>Pinboard ()</title>
    <link>http://pinboard.in/u:</link>
    <description>popular items from Pinboard</description>
    <items>
      <rdf:Seq>	<rdf:li rdf:resource='http://duartes.org/gustavo/blog/post/what-your-computer-does-while-you-wait'/>
	<rdf:li rdf:resource='http://www.gutenberg.org/wiki/Main_Page'/>
	<rdf:li rdf:resource='http://meld.sourceforge.net/'/>
       </rdf:Seq>
    </items>
  </channel><item rdf:about='http://duartes.org/gustavo/blog/post/what-your-computer-does-while-you-wait'>
    <title>What Your Computer Does While You Wait : Gustavo Duarte</title>
    <dc:date>2010-12-28T06:55:53+00:00</dc:date>
    <link>http://duartes.org/gustavo/blog/post/what-your-computer-does-while-you-wait</link>
    <dc:creator></dc:creator>
    <description><![CDATA[  ]]></description>
    <dc:subject>reference to-read </dc:subject>
    <taxo:topics>
      <rdf:Bag>
      	<rdf:li rdf:resource='http://pinboard.in/u:/t:reference'/>
	<rdf:li rdf:resource='http://pinboard.in/u:/t:to-read'/>
        </rdf:Bag>
      </taxo:topics>
    </item><item rdf:about='http://lamb.cc/typograph/'>
    <title>Typograph (lamb.cc)</title>
    <dc:date>2010-12-28T06:55:53+00:00</dc:date>
    <link>http://lamb.cc/typograph/</link>
    <dc:creator></dc:creator>
    <description><![CDATA[ Typography scale and vertical tempo.
 ]]></description>
    <dc:subject>typography webdesigntools webdesign </dc:subject>
    <taxo:topics>
      <rdf:Bag>
      	<rdf:li rdf:resource='http://pinboard.in/u:/t:typography'/>
	<rdf:li rdf:resource='http://pinboard.in/u:/t:webdesigntools'/>
	<rdf:li rdf:resource='http://pinboard.in/u:/t:webdesign'/>
        </rdf:Bag>
      </taxo:topics>
    </item>
   </rdf:RDF>
")

(fact
  (parse-rss-posts popular-xml) =>
  (just [(contains {:url "http://duartes.org/gustavo/blog/post/what-your-computer-does-while-you-wait"
		    :tags ["reference" "to-read"]})
	 (contains {:url "http://lamb.cc/typograph/"
		    :desc " Typography scale and vertical tempo. "
		    :tags ["typography" "webdesigntools" "webdesign"]})]))

(fact
 ;; Dec 28 06:55:53 UTC 2010
 (.getTime (parse-date rss-date-format "2010-12-28T06:55:53+00:00")) => 1293519353000)


;; Tests for the extended pinboard methods

(def a-note "
<?xml version='1.0' encoding='UTF-8' ?>
<note id='b71145a1731695b4ee23'>
    <hash>230c1b958ba91ab37a68</hash>
    <length>19</length>
    A somewhat longer title - at least a little longer!
    <text><![CDATA[test test test test]]></text>
</note>")

(fact
  (parse-pinboard-note a-note) =>
  [{:title "A somewhat longer title - at least a little longer!"
   :id "b71145a1731695b4ee23"
   :hash "230c1b958ba91ab37a68"
   :length "19"
   :text "test test test test"}])