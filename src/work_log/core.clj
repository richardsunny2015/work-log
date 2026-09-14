(ns work-log.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint])
  (:import [java.io PushbackReader]))

(def work-log-dir "work-log-files/")

(defn edn-file->map [file-name]
  (with-open [r (io/reader file-name)]
    (edn/read (PushbackReader. r))))

(defn maybe-setup-files []
  (let [d (io/file work-log-dir)]
    (when-not (and (.exists d) (.isDirectory d))
      (.mkdir d)
      (spit (str work-log-dir "tasks.edn") {})
      (spit (str work-log-dir "categories.edn") {}))))

(defn add-task [[task & [category]]]
  (let [task-file (str work-log-dir "tasks.edn")
        tasks (edn-file->map task-file)
        categories (edn-file->map (str work-log-dir "categories.edn"))
        category-exists? (and (not (nil? category)) (contains? categories category))
        task-map (cond-> {:name task
                          :elapsed-time 0}
                   category-exists? (assoc :category category))]
    (when (contains? tasks task)
      (throw (ex-info "Task already exists"
                      {:existing-task task})))
    (spit task-file (with-out-str (pprint/pprint (assoc tasks task task-map))))
    (println "Added task" task)
    (when (and (not (nil? category))
               (not category-exists?))
      (println "Task added without non-existent category" category))))

(defn add-category [[category]]
  (let [categories-file (str work-log-dir "categories.edn")
        categories (edn-file->map categories-file)
        category-map {:name category}]
    (when (contains? categories category)
      (throw (ex-info "Category already exists"
                      {:existing-category category})))
    (spit categories-file (with-out-str (pprint/pprint
                                         (assoc categories category category-map))))))

(defn add [[obj & args]]
  (case obj
    "task" (add-task args)
    "category" (add-category args)
    (throw (ex-info "Unknown object to add"
                    {:obj obj}))))

(defn start-task [[task]])

