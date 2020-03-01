(ns tangerina.main.lacinia.mutations
  (:require [datomic.api :as d]
            [datascript.core :as ds]))

(defn define-task
  [context args value]
  args)

(def define-tasks-edn
  `{:defineTask {:type    :Task
                 :resolve ~define-task}})

(def mutations-edn
  (merge define-tasks-edn))
