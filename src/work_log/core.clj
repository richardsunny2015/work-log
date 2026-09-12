(ns work-log.core
  (:require [clojure.java.io :as io]))

(def work-log-dir "work-log-files/")

(defn maybe-setup-files []
  (let [d (io/file work-log-dir)]
    (when-not (and (.exists d) (.isDirectory d))
      (.mkdir d)
      (spit (str work-log-dir "tasks.edn") {})
      (spit (str work-log-dir "categories.edn") {}))))

(defn add-task [[task & [category]]])

(defn add-category [[category]])

(defn add [[obj & args]]
  (case obj
    "task" (add-task args)
    "category" (add-category args)
    (throw (ex-info "Unknown object to add"
                    {:obj obj}))))

(defn start-task [[task]])

