(ns tangerina.main.datascript.queries
  (:require [tangerina.main.datascript.core :as db]
            [datascript.core :as ds]))

(defn q->tasks [q]
  (reduce (fn [acc [id d c]] (conj acc {:db/id id
                                       :task/description d
                                       :task/completed c}))
          [] q))

(def all-tasks
  '[:find ?id ?d ?c
    :in $
    :where
    [?id :task/description ?d]
    [?id :task/completed ?c]])

(defn get-all-tasks-db [db]
  (->> (ds/q all-tasks db)
     q->tasks
     (sort-by :db/id)))

(defn get-all-tasks [{::db/keys [conn]}]
  (->> (ds/q all-tasks (ds/db conn))
     q->tasks
     (sort-by :db/id)))

(def id->task
  '[:find ?id ?d ?c
    :in $ ?id
    :where
    [?id :task/description ?d]
    [?id :task/completed ?c]])

(defn get-task-by-id-db
  [db id]
  (->> (ds/q id->task db id)
     q->tasks
     first))

(defn get-task-by-id
  [{::db/keys [conn]} id]
  (let [db (ds/db conn)]
    (get-task-by-id-db db id)))

(defn  get-tasks-by-ids-db
  [db ids-data]
  (->> ids-data
     (sort-by :db/id)
     (map :db/id)
     (remove nil?)
     (map (partial get-task-by-id-db db))))

(comment
  ;;TODO test!
  ;;(is
  ;; (empty? (q/get-tasks-by-ids-db
  ;;          (ds/db conn)
  ;;          [{:db/id nil}])))
  )

(defn get-tasks-by-ids
  [{::db/keys [conn]} ids]
  (let [db (ds/db conn)]
    (get-tasks-by-ids-db db ids)))
