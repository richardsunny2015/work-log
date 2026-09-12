(ns work-log.core-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [work-log.core :as core]
            [work-log.fixture :refer [test-fixture]])
  (:import [clojure.lang ExceptionInfo]))

(use-fixtures :each test-fixture)

(deftest add-test
  (testing "throws error with invalid obj"
    (is (thrown-with-msg? ExceptionInfo #"Unknown object to add"
                          (core/add ["invalid"])))
    ))