(ns work-log.core-test
  (:require [clojure.test :refer [deftest testing is use-fixtures]]
            [work-log.core :as core]
            [work-log.fixture :refer [test-fixture test-dir]])
  (:import [clojure.lang ExceptionInfo]))

(use-fixtures :each test-fixture)

(deftest add-test
  (testing "throws error with invalid obj"
    (is (thrown-with-msg? ExceptionInfo #"Unknown object to add"
                          (core/add ["invalid"]))))
  (let [task "Code work-log"]
    (testing "adds task if previously non-existent"
      (let [expected {:name task
                      :elapsed-time 0}
            _ (core/add ["task" task])
            result (-> (core/edn-file->map (str test-dir "tasks.edn"))
                       (get task))]

        (is (= expected result))))
    (testing "throws error for already existing task"
      (is (thrown-with-msg? ExceptionInfo #"Task already exists"
                            (core/add ["task" task])))))
  (let [category "programming"]
    (testing "adds category if previously non-existent"
      (let [expected {:name category}
            _ (core/add ["category" category])
            result (-> (core/edn-file->map (str test-dir "categories.edn"))
                       (get category))]
        (is (= expected result))))
    (testing "throws error for already existing category"
      (is (thrown-with-msg? ExceptionInfo #"Category already exists"
                            (core/add ["category" category]))))
    (testing "adds task with category if specified"
      (let [task "new task"
            expected {:name task
                      :elapsed-time 0
                      :category category}
            _ (core/add ["task" task category])
            result (-> (core/edn-file->map (str test-dir "tasks.edn"))
                       (get task))]
        (is (= expected result)))))
  (testing "adds task without category if category does not exist"
    (let [task "task with non existent category"
          category "non existent"
          expected {:name task
                    :elapsed-time 0}
          _ (core/add ["task" task category])
          result (-> (core/edn-file->map (str test-dir "tasks.edn"))
                     (get task))]
      (is (= expected result)))))