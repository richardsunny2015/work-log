(ns work-log.core
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as cstr]
            [medley.core :as m])
  (:import [java.io PushbackReader]))

(def work-log-dir (str (System/getProperty "user.home") "/work-log-files/"))
(def tasks-file (str work-log-dir "tasks.edn"))
(def categories-file (str work-log-dir "categories.edn"))

(defn- edn-file->map [file-name]
  (with-open [r (io/reader file-name)]
    (edn/read (PushbackReader. r))))

(defn get-tasks []
  (edn-file->map tasks-file))

(defn get-categories []
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

(defn display-time [seconds]
  (format "%02d:%02d:%02d"
          (quot seconds (* 60 60))
          (mod (quot seconds 60) 60)
          (mod seconds 60)))

(defn- map->display-row
  "Takes a map and renames keys to make it displayable.
   Example: :elapsed-time -> 'Elapsed Time'"
  [x-map]
  (->> (update x-map :elapsed-time display-time)
       (m/map-keys
        (fn [k]
          (-> k
              (name)
              (cstr/split #"-")
              (->> (map cstr/capitalize)
                   (cstr/join #" ")))))))

(defn- xform-category
  [tasks category]
  (let [elapsed-time (->> (get tasks (:name category))
                          (map :elapsed-time)
                          (reduce + 0))]
    (map->display-row (assoc category :elapsed-time elapsed-time))))

(defn- validate-edit-category
  [categories category name]
  (when (nil? name)
    (throw (ex-info "No name inputted for category edit"
                    {:category category})))
  (when-not (contains? categories category)
    (throw (ex-info "Invalid category for edit"
                    {:category category})))
  (when (contains? categories name)
    (throw (ex-info "Name already in use for category edit"
                    {:category category
                     :name name}))))

(defn- validate-edit-task
  "Validates arguments for editting a task"
  [tasks categories task {:keys [category name] :as _options}]
  (when-not (contains? tasks task)
    (throw (ex-info "Invalid task for edit"
                    {:inputted-task task})))
  (when (and (not (nil? category)) (not (contains? categories category)))
    (throw (ex-info "Invalid category for task edit"
                    {:inputted-task task
                     :category category})))
  (when (and (not (nil? name)) (contains? tasks name))
    (throw (ex-info "Name already in use for task edit"
                    {:inputted-task task
                     :name name}))))

(defn- add-task [[task & [category]]]
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

(defn- add-category [[category]]
  (let [categories (get-categories)
        category-map {:name category}]
    (when (contains? categories category)
      (throw (ex-info "Category already exists"
                      {:existing-category category})))
    (spit-map-into-file categories-file categories
                        category category-map)))

(defn- list-tasks []
  (let [tasks (->> (get-tasks)
                   vals
                   (map map->display-row))]
    (pprint/print-table ["Name" "Category" "Elapsed Time"] tasks)))

(defn- list-categories []
  (let [tasks (->> (get-tasks)
                   (vals)
                   (group-by :category))
        categories (->> (get-categories)
                        (vals)
                        (map (partial xform-category tasks)))]
    (pprint/print-table ["Name" "Elapsed Time"] categories)))

(defn- edit-task [[task] {:keys [category name] :as options}]
  (let [tasks (get-tasks)
        categories (get-categories)
        task-map (get tasks task)]
    (validate-edit-task tasks categories task options)
    (spit tasks-file
          (with-out-str
            (pprint/pprint
             (-> tasks
                 (dissoc task) ;; remove task because task may be renamed
                 (assoc (or name task)
                        (m/assoc-some task-map
                                      :name name
                                      :category category))))))))

(defn- edit-category [[category] {:keys [name]}]
  (let [categories (get-categories)
        tasks (get-tasks)]
    (validate-edit-category categories category name)
    (spit categories-file
          (with-out-str
            (pprint/pprint
             (-> categories
                 (dissoc category)
                 (assoc name {:name name})))))
    (spit tasks-file
          (with-out-str
            (pprint/pprint
             (m/map-vals
              (fn [task-map]
                (cond-> task-map
                  (= category (:category task-map))
                  (assoc :category name)))
              tasks))))))

;; API

(defn list-objects [[obj]]
  (case obj
    "task" (list-tasks)
    "category" (list-categories)
    (throw (ex-info "Unknown object to list"
                    {:obj obj}))))

(defn add [[obj & args]]
  (case obj
    "task" (add-task args)
    "category" (add-category args)
    (throw (ex-info "Unknown object to add"
                    {:obj obj}))))

(defn edit [[obj & args] options]
  (case obj
    "task" (edit-task args options)
    "category" (edit-category args options)
    (throw (ex-info "Unknown object to edit"
                    {:obj obj}))))

(defn start-task [[task]]
  (let [tasks (get-tasks)
        seconds (atom 0)]
    (when-not (contains? tasks task)
      (throw (ex-info "Task does not exist. Use `add task TASK`."
                      {:entered-task task})))
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. (close-program tasks task seconds)))
    (println "Shutdown program to save time elapsed.")
    (loop []
      (print (str "\r" (display-time @seconds)))
      (Thread/sleep 1000)
      (swap! seconds inc)
      (flush)
      (recur))))

