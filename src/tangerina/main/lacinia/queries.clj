(ns tangerina.main.lacinia.queries)

(defn resolve-hello
  [context args value]
  "Hello, Clojurians!")

(def hello-edn
  `{:hello {:type    ~'String
            :resolve ~resolve-hello}})

(defn list-tasks
  [context args value]
  (prn args)
  )

(def list-tasks-edn
  `{:listTasks {:args    {:id {:type ~'ID}}
                :type    (~'list :Task)
                :resolve ~list-tasks}})

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
