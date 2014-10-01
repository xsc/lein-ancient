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
  {:f (constantly ["1.0.0"])}
  {:uri "http://localhost:12343" :username "abc" :passphrase "def"}
  {:uri "https://localhost:12343" :username "abc" :passphrase "def"}
  {:uri "file:///tmp/repo"}
  {:url "file:///tmp/repo"}
  {:uri "s3p://maven/bucket" :username "abc" :passphrase "def"}
  {:uri "s3p://maven/bucket" :username "abc" :password "def"}
  {:url "s3p://maven/bucket" :username "abc" :passphrase "def"})

(tabular
  (fact "about invalid loader specifications."
        (loader-for ?v) => (throws ?ex))
  ?v                          ?ex
  ""                          IllegalArgumentException
  "invalid://abc"             IllegalArgumentException
  "s3p://"                    AssertionError
  "s3p://maven/bucket"        AssertionError
  {}                          Exception
  {:uri "s3p://maven/bucket"} AssertionError
  {:uri "s3p://maven/bucket"
   :username "abc"}           AssertionError)

(fact "about loader TTL."
      (let [touch (atom 0)
            loader (loader-for
                     {:f (fn [& _]
                           (swap! touch inc)
                           ["1.0.0"])
                      :ttl 200})]
        loader => fn?
        (loader "group" "id") => ["1.0.0"]
        (loader "group" "id") => ["1.0.0"]
        @touch => 1
        (Thread/sleep 250)
        (loader "group" "id") => ["1.0.0"]
        (loader "group" "id") => ["1.0.0"]
        @touch => 2))

(fact "about loader LRU."
      (let [touch (atom 0)
            loader (loader-for
                     {:f (fn [& _]
                           (swap! touch inc)
                           ["1.0.0"])
                      :lru 1})]
        loader => fn?
        (loader "group" "id") => ["1.0.0"]
        (loader "group" "id") => ["1.0.0"]
        @touch => 1
        (loader "group" "id2") => ["1.0.0"]
        @touch => 2
        (Thread/sleep 250)
        (loader "group" "id2") => ["1.0.0"]
        @touch => 2))

(fact "about loader LRU + TTL."
      (let [touch (atom 0)
            loader (loader-for
                     {:f (fn [& _]
                           (swap! touch inc)
                           ["1.0.0"])
                      :ttl 200
                      :lru 1})]
        loader => fn?
        (loader "group" "id") => ["1.0.0"]
        (loader "group" "id") => ["1.0.0"]
        @touch => 1
        (loader "group" "id2") => ["1.0.0"]
        @touch => 2
        (Thread/sleep 250)
        (loader "group" "id2") => ["1.0.0"]
        @touch => 3))
