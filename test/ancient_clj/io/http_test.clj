(ns ancient-clj.io.http-test
  (:require [midje.sweet :refer :all]
            [ancient-clj.io
             [http :refer [http-loader]]
             [xml :refer [metadata-path]]
             [xml-test :as xml]]
            [org.httpkit.server :refer [run-server]])
  (:import [org.apache.commons.codec.binary Base64]))

(def loader
  (http-loader "http://localhost:12343"))

(def basic-auth-loader
  (http-loader "http://localhost:12343"
               {:username "user"
                :passphrase "pass"}))

(let [server (atom nil)
      request (atom nil)]
  (with-state-changes [(before :facts (->> (run-server
                                             (let [xml (xml/generate-xml)]
                                               (fn [req]
                                                 (reset! request req)
                                                 {:status 200
                                                  :headers {"content-type" "text/xml"}
                                                  :body xml}))
                                             {:port 12343})
                                           (reset! server)))
                       (after :facts (do
                                       (reset! request nil)
                                       (@server)))]
    (fact "about loading versions from HTTP/XML repositories."
          (let [vs (set (loader "group" "id"))]
            vs => (has every? string?)
            (count vs) => (count xml/versions)
            xml/snapshot-versions => (has every? vs)
            xml/qualified-versions => (has every? vs)
            xml/release-versions => (has every? vs)
            (:uri @request) => (str "/" (metadata-path "group" "id"))))
    (fact "about HTTP basic authentication."
          (let [vs (basic-auth-loader "group" "id")
                {:keys [headers]} @request
                auth (some headers ["authorization" "Authorization"])]
            vs => (has every? string?)
            (count vs) => (count xml/versions)
            (- (count auth) 6) => pos?
            (subs auth 0 5) => "Basic"
            (subs auth 6) => (-> "user:pass"
                                 (.getBytes "UTF-8")
                                 (Base64/encodeBase64String))))))

(let [server (atom nil)
      throwable? (fn [msg]
                   (fn [t]
                     (and (instance? Throwable t)
                          (.contains (.getMessage t) msg))))]
  (tabular
    (with-state-changes [(before :facts (->> (run-server
                                               (constantly ?response)
                                               {:port 12343})
                                             (reset! server)))
                         (after :facts (@server))]
      (fact "about missing/invalid artifacts"
            (loader "group" "id") => ?check))
    ?response                                 ?check
    {:status 404}                             empty?
    {:status 503}                             (throwable? "status=503")
    {:status 200
     :headers {"content-type" "text/xml"}}    (throwable? "Could not parse metadata XML")
    {:status 200
     :headers {"content-type" "text/xml"}
     :body "<not-xml>"}                       (throwable? "Could not parse metadata XML")
    {:status 200
     :headers {"content-type" "text/plain"}
     :body (xml/generate-xml)}                (throwable? "content-type is not XML")
    {:status 200}                             (throwable? "content-type is not XML"))

  (fact "about connection failure."
        (loader "group" "id") => (throwable? "Connection refused")))
