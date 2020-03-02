(ns tangerina.main.lacinia.mutations
  (:require [datascript.core :as ds]
            [medley.core :as medley]))
#_(let [schema (ds-schema)
        conn   (ds/create-conn schema)]
    (ds/transact! conn [{:task/description "Foo"
                         :task/completion  false}])
    (ds/transact! conn [{:task/description "Foo"
                         :task/completion  false}])
    (-> (ds/transact! conn [{:task/description "Foo"
                             :task/completion  false}])
        :tempids ffirst)
    #_(->> (ds/q '[:find ?d ?c
                   :in $ ?id
                   :where
                   [?id :task/description ?d]
                   [?id :task/completion ?c]] (ds/db conn))

           (map #(zipmap [:task/description
                          :task/completion] %))))

(defn args->tx-data
  [args]
  (->> args
     (medley/map-keys #(keyword "task" (name %)))))

(defn tx-task!
  [conn args]
  (->> args
     args->tx-data
     (ds/transact! conn)))

(defn tx-task!->id
  [tx]
  (-> tx :tempids ffirst))

(defn find-task [db id]
  (ds/q '[:find ?d ?c
          :in $ ?id
          :where
          [?id :task/description ?d]
          [?id :task/completion ?c]] db id))

(defn define-task
  [context args _value]
  #_(let [tx (tx-task!  args)
          id (tx-task!->id tx)
          db (tx :db-after)]
      (find-task db id)))

(def define-task-edn
  `{:defineTask {:args    {:description {:type ~'String}
                           :completion  {:type ~'Boolean}}
                 :type    :Task
                 :resolve ~define-task}})

(def mutations-edn
  (merge define-task-edn))
