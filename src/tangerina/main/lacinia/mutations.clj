(ns tangerina.main.lacinia.mutations
  (:require [tangerina.main.datascript.queries :as q]
            [tangerina.main.datascript.transactions :as tx]))

#_(-> tangerina.main.core/state-server
      deref
      :datascript/conn
      ds/db
      get-all-tasks)


#_(def define-task-edn
    `{:defineTask {:args    {:id          {:type ~'ID}
                             :description {:type ~'String}
                             :completed   {:type ~'Boolean}
                             :delete      {:type ~'Boolean}}
                   :type    :Task
                   :resolve ~define-task!}})

#_(def mutations-edn
    (merge define-task-edn))
