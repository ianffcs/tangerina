(ns tangerina.main.datascript
  (:require [datascript.core :as ds]))

(def schema
  {:task/description {:valueType   :string
                      :cardinality :one}
   :task/checked     {:valueType   :boolean
                      :cardinality :one}})

(defn create-task
  ([description]
   (let [tempid (ds/tempid :db.part/user)]
     (create-task description tempid)))
  ([description tempid]
   [[:db/add tempid :task/description description]
    [:db/add tempid :task/checked false]]))

(defn uncomplete-task
  [id]
  [[:db/add id :task/checked false]])

(defn complete-task
  [id]
  [[:db/add id :task/checked true]])

(defn update-task
  [id description]
  [[:db/add id :task/description description]])

(defn delete-task
  [id]
  [[:db.fn/retractEntity id]])

(defn tasks
  [db]
  (ds/q '[:find [(pull ?e [*]) ...]
          :where
          [?e :task/description]]
        db))

(defn get-task-by-id
  [db id]
  (ds/q '[:find [(pull ?id [*]) ...]
          :in $ ?id
          :where
          [?id :task/description]]
        db id))

(defn datascript-impl
  [{:tangerina.main.core/keys [conn]}]
  {:query/tasks          (fn [_ _ _]
                           (->> tasks
                              (map (partial zipmap [:id :description :checked]))))
   :query/impl           (constantly "datascript")
   :mutation/create-task (fn [_ {:keys [description]} _]
                           (let [id                     (ds/tempid :db.part/user)
                                 {:keys [db-after tempids]} (ds/transact! conn [{:db/id            id
                                                                             :task/checked     false
                                                                             :task/description description}])]
                             {:id          (ds/resolve-tempid db-after tempids id)
                              :checked     false
                              :description description}))})
