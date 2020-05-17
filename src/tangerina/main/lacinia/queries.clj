(ns tangerina.main.lacinia.queries
  (:require [tangerina.main.datascript.queries :as q]))

(defn resolve-hello
  [context args value]
  "Hello, Clojurians!")

(def hello-edn
  `{:hello {:type    ~'String
            :resolve ~resolve-hello}})

(defn get-task!
  [system args _value]
  (q/get-task-by-id system {:db/id (-> args
                                      (select-keys [:id])
                                      :id
                                      Integer/parseInt)}))

#_(defn list-tasks
    [system args _value]
    )

(def queries-edn
  (merge `{:getTask       {:args    {:id {:type ~'ID}}
                           :type    (~'list :Task)
                           :resolve ~get-task!}
           #_#_:listTasks {:args    {:id {:type ~'ID}}
                           :type    (~'list :Task)
                           :resolve ~list-tasks!}} hello-edn))
