(ns ancient-clj.io.http
  (:require [ancient-clj.io.xml :as xml]
            [clj-http.client :as http]))

(def ^:private error-messages
  {401 "authorization needed."
   403 "access forbidden."
   500 "server error."
   502 "upstream server error."
   503 "server unavailable."
   504 "server timeout."})

(def ^:private valid-content-types
  #{"text/xml" "application/xml"})

(defn- base-opts
  []
  (let [timeout (if-let [v (System/getenv "http_timeout")]
                  (if (not= v "0")
                    (Long/parseLong v)
                    (* 60 60 1000))
                  (if (some
                        #(System/getenv %)
                        ["http_proxy" "https_proxy"])
                    30000
                    5000))]
    {:socket-timeout timeout
     :conn-timeout timeout
     :throw-exceptions false
     :as :text}))

(defn http-loader
  "Create version loader for a HTTP repository."
  [repository-uri & [{:keys [username passphrase timeout]}]]
  (let [opts (merge
               (base-opts)
               (when (string? username)
                 {:basic-auth [username passphrase]})
               (when timeout
                 {:socket-timeout timeout
                  :conn-timeout timeout}))]
    (fn [group id]
      (try
        (let [uri (xml/metadata-uri repository-uri group id)
              {:keys [status headers body error]} (http/get uri opts)
              {:keys [content-type]} headers
              content-type (and content-type (first (.split content-type ";")))]
          (if-not error
            (if (= status 200)
              (if (contains? valid-content-types content-type)
                (if (string? body)
                  (xml/metadata-xml->versions body)
                  (Exception. "invalid response body."))
                (Exception.
                  (format "response content-type is not XML (%s): %s"
                          (pr-str valid-content-types)
                          content-type)))
              (if (or (< status 300) (= status 404))
                []
                (Exception.
                  (format "[status=%d] %s"
                          status
                          (error-messages status "request failed.")))))
            error))
        (catch Throwable ex
          ex)))))
