(ns ancient-clj.io-test
  (:require [midje.sweet :refer :all]
            [ancient-clj.io :refer :all]))

(tabular
  (fact "about loader creation."
        (loader-for ?v) => fn?)
  ?v
  "http://localhost:12343"
  "https://localhost:12343"
  "file:///tmp/repo"
  "file:repo"
  "file:../repo"
  {:f (constantly ["1.0.0"])}
  {:uri "http://localhost:12343" :username "abc" :passphrase "def"}
  {:uri "https://localhost:12343" :username "abc" :passphrase "def"}
  {:uri "file:///tmp/repo"}
  {:url "file:///tmp/repo"}
  {:url "file:repo"}
  {:url "file:../repo"}
  {:uri "s3p://maven/bucket" :username "abc" :passphrase "def"}
  {:uri "s3p://maven/bucket" :username "abc" :password "def"}
  {:url "s3p://maven/bucket" :username "abc" :passphrase "def"}
  {:uri "s3://maven/bucket" :username "abc" :passphrase "def"}
  ;; uses default credentials
  "s3p://maven/bucket"
  {:uri "s3p://maven/bucket"}
  {:uri "s3://maven/bucket"})

(tabular
  (fact "about invalid loader specifications."
        (loader-for ?v) => (throws ?ex))
  ?v                          ?ex
  ""                          IllegalArgumentException
  "invalid://abc"             IllegalArgumentException
  "s3p://"                    AssertionError
  "file://repo"               AssertionError
  {}                          IllegalArgumentException
  {:uri "s3p://maven/bucket"
   :username "abc"}           IllegalArgumentException
  {:uri "s3p://maven/bucket"
   :passphrase "def"}         IllegalArgumentException)
