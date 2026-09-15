(ns work-log.main
  (:gen-class)
  (:require [clojure.tools.cli :refer [parse-opts]]
            [work-log.core :as core]))

(defn -main [& args]
  (core/maybe-setup-files)
  (let [{:keys [arguments options errors]}
        (parse-opts args [["-c" "--category CATEGORY" "Category for edit"]
                          ["-n" "--name NAME" "Name for edit"]])
        operation (first arguments)
        arguments (rest arguments)]
    (when-not (empty? errors)
      (throw (ex-info "Errors parsing command"
                      {:errors errors})))
    (case operation
      "start" (core/start-task arguments)
      "add" (core/add arguments)
      "list" (core/list-objects arguments)
      "edit" (core/edit arguments options)
      (throw (ex-info "Unsupported operation"
                      {:operation operation})))))