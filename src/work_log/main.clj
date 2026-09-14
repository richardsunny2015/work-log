(ns work-log.main
  (:gen-class)
  (:require [work-log.core :as core]))

(defn -main [& args]
  (core/maybe-setup-files)
  (let [operation (first args)]
    (case operation
      "start" (core/start-task (rest args))
      "add" (core/add (rest args))
      (throw (ex-info "Unsupported operation"
                      {:operation operation})))))