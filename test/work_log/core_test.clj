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

(deftest edit-test
  (let [task1 "testing"
        task2 "test2"
        task3 "test3"
        category "test"
        new-name1 "new name"
        new-name2 "new name two"]
    (core/add ["task" task1])
    (core/add ["task" task2])
    (core/add ["task" task3])
    (core/add ["category" category])
    (testing "throws error for invalid object"
      (is (thrown-with-msg? ExceptionInfo #"Unknown object to edit"
                            (core/edit ["invalid object"] {}))))
    (testing "throws error for non existing task"
      (is (thrown-with-msg? ExceptionInfo #"Invalid task for edit"
                            (core/edit ["task" "invalid"] {}))))
    (testing "throws error for non existing category"
      (is (thrown-with-msg? ExceptionInfo #"Invalid category for edit"
                            (core/edit ["category" "invalid"] {:name "new-name"}))))
    (testing "throws error for category with new name that already exists"
      (is (thrown-with-msg? ExceptionInfo #"Name already in use for category edit"
                            (core/edit ["category" category] {:name category}))))
    (testing "throws error for no name for category"
      (is (thrown-with-msg? ExceptionInfo #"No name inputted for category edit"
                            (core/edit ["category" category] {}))))
    (testing "throws error when editting task with invalid category"
      (is (thrown-with-msg? ExceptionInfo #"Invalid category for task edit"
                            (core/edit ["task" task1] {:category "invalid category"}))))
    (testing "throws error when editting name of task with already existing task"
      (is (thrown-with-msg? ExceptionInfo #"Name already in use for task edit"
                            (core/edit ["task" task1] {:name task2}))))
    (testing "edits task correctly"
      (testing "edits category field"
        (core/edit ["task" task1] {:category category})
        (let [task-map (-> (core/get-tasks)
                           (get task1))
              expected {:name task1
                        :category category
                        :elapsed-time 0}]
          (is (= expected task-map))))
      (testing "edits name field"
        (core/edit ["task" task1] {:name new-name1})
        (let [tasks (core/get-tasks)
              expected {:name new-name1
                        :category category
                        :elapsed-time 0}]
          (is (not (contains? tasks task1)))
          (is (= expected (get tasks new-name1)))))
      (testing "edits multiple fields"
        (core/edit ["task" task2] {:name new-name2
                                   :category category})
        (let [tasks (core/get-tasks)
              expected {:name new-name2
                        :category category
                        :elapsed-time 0}]
          (is (not (contains? tasks task2)))
          (is (= expected (get tasks new-name2))))))
    (testing "edits category field correctly"
      (let [new-category-name "new category"
            _ (core/edit ["category" category] {:name new-category-name})
            tasks (core/get-tasks)
            tasks-with-category (-> tasks
                                    (select-keys [new-name1 new-name2])
                                    (vals))
            categories (core/get-categories)
            expected-category {:name new-category-name}]
        (is (= expected-category (get categories new-category-name)))
        (is (not (contains? categories category)))
        (is (= 2 (count tasks-with-category)))
        (is (every? #(= new-category-name %)
                    (map :category tasks-with-category)))
        (is (not= new-category-name (get tasks task3)))))))