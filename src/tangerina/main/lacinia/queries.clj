(ns tangerina.main.lacinia.queries
  (:require [datascript.core :as ds]))

(defn resolve-hello
  [context args value]
  "Hello, Clojurians!")

(def hello-edn
  `{:hello {:type    ~'String
            :resolve ~resolve-hello}})

(defn find-task
  ([db]
   (map (partial zipmap [:id :description :completed])
        (ds/q '[:find ?id ?d ?c
                :where
                [?id :description ?d]
                [?id :completed ?c]] db)))
  ([db id]
   (->> (ds/q '[:find ?id ?d ?c
              :in $ ?id
              :where
              [?id :description ?d]
              [?id :completed ?c]] db id)
      first
      (zipmap [:id :description :completed]))))

(defn list-tasks!
  [{:datascript/keys [conn]} args _value]
  (let [{:keys [id]} args
        parsed-id    (when id (read-string id))
        db           (ds/db conn)]
    (if id
      [(update (find-task db parsed-id) :id str)]
      (map #(update % :id str) (find-task db)))))

(def list-tasks-edn
  `{:listTasks {:args    {:id {:type ~'ID}}
                :type    (~'list :Task)
                :resolve ~list-tasks!}})

(def queries-edn
  (merge list-tasks-edn hello-edn))


(comment (def query-schema '[^{:lacinia/tag-recursive true
                               :lacinia/query         true}
                             QueryRoot
                             [^{:type            ToDo
                                :cardinality     [n]
                                :lacinia/resolve :query/all-todos} allTodos]])

         (defn ^:private all-todos [context args res-value]
           [{:description "else"
             :createdAt   #inst "2019-01-01"}
            {:description "somethig"
             :createdAt   #inst "2019-01-02"}])

         (def query-resolvers-map
           {:query/all-todos all-todos})

         (def mutation-schema '[^{:lacinia/tag-recursive true
                                  :lacinia/mutation      true}
                                MutationRoot
                                [^{:type            ToDo
                                   :lacinia/resolve :mutation/finish-todo}
                                 finishTodo
                                 [^{:type String} id]]])

         (defn ^:private finish-todo [context {:keys [id]} _]
           (identity id))

         (def mutation-resolvers-map
           {:mutation/finish-todo finish-todo}))
