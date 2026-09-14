(ns work-log.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint])
  (:import [java.io PushbackReader]))

(def work-log-dir "work-log-files/")
(def tasks-file (str work-log-dir "tasks.edn"))
(def categories-file (str work-log-dir "categories.edn"))

(defn- edn-file->map [file-name]
  (with-open [r (io/reader file-name)]
    (edn/read (PushbackReader. r))))

(defn get-tasks
  []
  (edn-file->map tasks-file))

(defn get-categories
  []
  (edn-file->map categories-file))

(defn- spit-map-into-file
  "Spits map into a file with kv assoc-ed into it."
  [filename x-map k v]
  (spit filename (with-out-str (pprint/pprint (assoc x-map k v)))))

(defn maybe-setup-files []
  (let [d (io/file work-log-dir)]
    (when-not (and (.exists d) (.isDirectory d))
      (.mkdir d)
      (spit tasks-file {})
      (spit categories-file {}))))

(defn- close-program
  "Takes tasks, task, and seconds atom to update task-file
   and returns a shutdown hook."
  [tasks task seconds]
  (fn []
    (let [task-map (-> tasks
                       (get task)
                       (update :elapsed-time + @seconds))]
      (spit-map-into-file tasks-file tasks task task-map))))

(defn display-time
  [seconds]
  (format "%02d:%02d:%02d"
          (quot @seconds (* 60 60))
          (mod (quot @seconds 60) 60)
          (mod @seconds 60)))

(defn add-task [[task & [category]]]
  (let [tasks (get-tasks)
        categories (get-categories)
        category-exists? (and (not (nil? category))
                              (contains? categories category))
        task-map (cond-> {:name task
                          :elapsed-time 0}
                   category-exists? (assoc :category category))]
    (when (contains? tasks task)
      (throw (ex-info "Task already exists"
                      {:existing-task task})))
    (spit-map-into-file tasks-file tasks task task-map)
    (println "Added task" task)
    (when (and (not (nil? category))
               (not category-exists?))
      (println "Task added without non-existent category" category))))

(defn add-category [[category]]
  (let [categories (get-categories)
        category-map {:name category}]
    (when (contains? categories category)
      (throw (ex-info "Category already exists"
                      {:existing-category category})))
    (spit-map-into-file categories-file categories
                        category category-map)))

(defn add [[obj & args]]
  (case obj
    "task" (add-task args)
    "category" (add-category args)
    (throw (ex-info "Unknown object to add"
                    {:obj obj}))))

(defn start-task [[task]]
  (let [tasks (get-tasks)
        seconds (atom 0)]
    (when-not (contains? tasks task)
      (throw (ex-info "Task does not exist"
                      {:task task})))
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (close-program tasks task seconds)))
    (loop []
      (print (str "\r" (display-time seconds)))
      (Thread/sleep 1000)
      (swap! seconds inc)
      (flush)
      (recur))))

