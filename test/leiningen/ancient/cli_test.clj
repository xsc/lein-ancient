(ns leiningen.ancient.cli-test
  (:require [leiningen.ancient.cli :refer :all]
            [clojure.test :refer :all]))

(deftest t-the-default-settings
  (are [?k ?v]
       (let [[flags rst] (parse [])]
         (is (empty? rst))
         (is (map? flags))
         (is (= ?v (flags ?k))))
       :check-clojure?   false
       :dependencies?    true
       :plugins?         false
       :profiles?        true
       :java-agents?     true
       :qualified?       false
       :snapshots?       false
       :interactive?     false
       :colors?          true
       :print?           false
       :recursive?       false
       :only             []
       :exclude          []
       :tests?           true))

(defmacro with-logging
  [b & body]
  `(binding [leiningen.core.main/*info* ~b
             *err* *out*]
     ~@body))

(deftest t-unknown-flags
  (is (thrown? clojure.lang.ExceptionInfo
               (re-pattern "not recognized")
               (with-logging false
                 (parse [":unknown"])))))

(deftest t-deprecated-flags
  (is (re-find #"no longer supported"
               (with-out-str
                                  (with-logging true
                                    (parse [":get"]))))))

(deftest t-unapplicable-flags
  (is (re-find
        #"not applicable"
         (with-out-str
           (with-logging true
             (parse [":print"] :exclude [:print]))))))

(deftest t-rest-arguments
  (are [?args ?rst]
       (let [[_ rst] (parse ?args)]
         (is (= ?rst rst)))
       ["hello"]                  ["hello"]
       ["hello" "world"]          ["hello" "world"]
       ["--" "hello"]             ["hello"]
       [":print" "hello" "world"] ["hello" "world"]
       [":print" "--" ":print"]   [":print"]))

(let [defaults (first (parse []))
      dis (partial apply dissoc)]
  (deftest t-flag-effects
    (are [?args ?effects]
         (let [[flags _] (parse ?args)
               ks (keys ?effects)]
           (is (= (dis defaults ks) (dis flags ks)))
           (is (= ?effects (select-keys flags ks))))
         [":all"]                 {:dependencies? true, :plugins? true,
                                   :java-agents? true}
         [":allow-all"]           {:snapshots? true, :qualified? true}
         [":allow-snapshots"]     {:snapshots? true}
         [":allow-qualified"]     {:qualified? true}
         [":check-clojure"]       {:check-clojure? true}
         [":interactive"]         {:interactive? true}
         [":plugins"]             {:dependencies? false, :plugins? true,
                                   :java-agents? false}
         [":all" ":plugins"]      {:dependencies? true, :plugins? true,
                                   :java-agents? true}
         [":plugins" ":all"]      {:dependencies? true, :plugins? true,
                                   :java-agents? true}
         [":java-agents"]         {:dependencies? false, :plugins? false,
                                   :java-agents? true}
         [":no-colors"]           {:colors? false}
         [":no-colours"]          {:colors? false}
         [":no-profiles"]         {:profiles? false}
         [":no-tests"]            {:tests? false}
         [":tests"]               {:tests? true}
         [":print"]               {:print? true, :tests? false}
         [":print" ":tests"]      {:print? true, :tests? true}
         [":tests" ":print"]      {:print? true, :tests? true}
         [":recursive"]           {:recursive? true})))

(let [defaults (first (parse []))
      dis (partial apply dissoc)]
  (deftest t-setting-effects
    (are [?args ?effects]
         (let [[flags args] (parse ?args)
               ks (keys ?effects)]
           (is (= (dis defaults ks) (dis flags ks)))
           (is (= ?effects (select-keys flags ks)))
           (is (empty? args)))
         [":only" "a,b"]          {:only [:a :b]}
         [":exclude" "x,y"]       {:exclude [:x :y]})))
