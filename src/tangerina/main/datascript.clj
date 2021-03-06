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

(defn uncheck-task
  [id]
  [[:db/add id :task/checked false]])

(defn check-task
  [id]
  [[:db/add id :task/checked true]])

(defn set-description-task
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

(defn get-tasks-by-ids
  [db ids]
  (reduce #(get-task-by-id db %) ids))

(defn deserialize-keys [{:db/keys   [id]
                         :task/keys [description
                                     checked]}]
  {:id          id
   :description description
   :checked     checked})

(defn serialize-keys
  [{:keys [id description checked]}]
  {:db/id            id
   :task/description description
   :task/checked     checked})

(defn datascript-impl
  [{:tangerina.main.core/keys [conn]}]
  {:query/tasks                   (fn [_ _ _]
                                    (->> (tasks (ds/db conn))
                                         (map deserialize-keys)
                                         (sort-by :id)))
   :query/impl                    (constantly "datascript")
   :mutation/create-task          (fn [_ {:keys [description]} _]
                                    (let [id                         (ds/tempid :db.part/user)
                                          data                       {:db/id            id
                                                                      :task/checked     false
                                                                      :task/description description}
                                          {:keys [db-after tempids]} (ds/transact! conn [data])]
                                      (-> data
                                          deserialize-keys
                                          (update :id #(ds/resolve-tempid db-after tempids %)))))
   :mutation/check-task           (fn [_ {:keys [id]} _]
                                    (let [{checked-bef :task/checked
                                           :as         task-bef} (first (get-task-by-id (ds/db conn) id))
                                          checking-data          (if checked-bef
                                                                   (uncheck-task id)
                                                                   (check-task id))]
                                      (ds/transact! conn checking-data)
                                      (deserialize-keys (update task-bef :task/checked not))))
   :mutation/set-description-task (fn [_ {:keys [id description]} _]
                                    (let [task-bef (first (get-task-by-id (ds/db conn) id))]
                                      (ds/transact! conn (set-description-task id description))
                                      (-> task-bef
                                          deserialize-keys
                                          (assoc :description description))))
   :mutation/delete-task          (fn [_ {:keys [id]} _]
                                    (let [task-bef (first (get-task-by-id (ds/db conn) id))]
                                      (ds/transact! conn (delete-task id))
                                      (deserialize-keys task-bef)))})
