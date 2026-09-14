(ns work-log.core-test
  (:require [clojure.pprint :as pprint]
            [clojure.test :refer [deftest testing is use-fixtures]]
            [work-log.core :as core]
            [work-log.fixture :refer [test-fixture tasks-file categories-file]])
  (:import [clojure.lang ExceptionInfo]))

(use-fixtures :each test-fixture)

(defn- print-table-stub [_ rows]
  rows)

(deftest add-test
  (testing "throws error with invalid obj"
    (is (thrown-with-msg? ExceptionInfo #"Unknown object to add"
                          (core/add ["invalid"]))))
  (let [task "Code work-log"]
    (testing "adds task if previously non-existent"
      (let [expected {:name task
                      :elapsed-time 0}
            _ (core/add ["task" task])
            result (-> (core/get-tasks)
                       (get task))]

        (is (= expected result))))
    (testing "throws error for already existing task"
      (is (thrown-with-msg? ExceptionInfo #"Task already exists"
                            (core/add ["task" task])))))
  (let [category "programming"]
    (testing "adds category if previously non-existent"
      (let [expected {:name category}
            _ (core/add ["category" category])
            result (-> (core/get-categories)
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
            result (-> (core/get-tasks)
                       (get task))]
        (is (= expected result)))))
  (testing "adds task without category if category does not exist"
    (let [task "task with non existent category"
          category "non existent"
          expected {:name task
                    :elapsed-time 0}
          _ (core/add ["task" task category])
          result (-> (core/get-tasks)
                     (get task))]
      (is (= expected result)))))

(deftest display-time-test
  (testing "should display correct time based on seconds"
    (is (= "00:00:00" (core/display-time 0)))
    (is (= "00:01:40" (core/display-time 100)))
    (is (= "01:00:00" (core/display-time (* 60 60))))
    (is (= "240:30:59" (core/display-time (+ (* 60 60 240)
                                             (* 60 30)
                                             59))))))

(deftest list-objects-test
  (testing "should throw error if object is unknown"
    (is (thrown-with-msg? ExceptionInfo #"Unknown object to list"
                          (core/list-objects ["invalid"]))))
  (let [test-tasks {"testing" {:name "testing"
                               :category "test"
                               :elapsed-time 100}
                    "no cat test" {:name "no cat test"
                                   :elapsed-time 30}
                    "also testing" {:name "also testing"
                                    :category "test"
                                    :elapsed-time 20}}
        test-categories {"test" {:name "test"}}]
    (spit tasks-file test-tasks)
    (spit categories-file test-categories)
    (with-redefs [pprint/print-table print-table-stub]
      (testing "should display tasks correctly"
        (let [expected [{"Name" "testing"
                         "Category" "test"
                         "Elapsed Time" "00:01:40"}
                        {"Name" "no cat test"
                         "Elapsed Time" "00:00:30"}
                        {"Name" "also testing"
                         "Category" "test"
                         "Elapsed Time" "00:00:20"}]
              result (core/list-objects ["task"])]
          (is (= expected result))))
      (testing "should display categories correctly"
        (let [expected [{"Name" "test"
                         "Elapsed Time" "00:02:00"}]]
          (is (= expected (core/list-objects ["category"]))))))))