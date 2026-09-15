(ns work-log.fixture
  (:require [clojure.java.io :as io]
            [work-log.core :as core]))

(def test-dir "test/work-log-files/")
(def tasks-file (str test-dir "tasks.edn"))
(def categories-file (str test-dir "categories.edn"))

(defn- cleanup [dir]
  (let [d (io/file dir)]
    (doseq [f (.listFiles d)]
      (io/delete-file f))
    (io/delete-file d)))

(defn test-fixture [f]
  (with-redefs [core/work-log-dir test-dir
                core/tasks-file tasks-file
                core/categories-file categories-file]
    (core/maybe-setup-files)
    (f)
    (cleanup test-dir)))