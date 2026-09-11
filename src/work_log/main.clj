(ns work-log.main
  (:gen-class))

(defn -main [& args]
  (println "args" (first args))
  (println "hello"))