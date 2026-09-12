(ns work-log.fixture
  (:require [clojure.java.io :as io]
            [work-log.core :as core]))

(defn- cleanup [dir]
  (let [d (io/file dir)]
    (doseq [f (.listFiles d)]
      (io/delete-file f))
    (io/delete-file d)))

(defn test-fixture [f]
  (let [test-dir (str "test/" core/work-log-dir)]
    (with-redefs [core/work-log-dir test-dir]
      (core/maybe-setup-files)
      (f)
      (cleanup test-dir))))