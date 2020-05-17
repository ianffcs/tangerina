(ns tangerina.main.datascript.transactions
  (:require [datascript.core :as ds]
            [tangerina.main.datascript.core :as db]
            [tangerina.main.datascript.queries :as q]))

(defn create-task
  [tx-data]
  (reduce (fn [acc val]
            (conj acc (-> val
                         (select-keys [:task/description])
                         (assoc :task/completed false)))) [] tx-data))

(defn create-task! [{::db/keys [conn]} tx-data]
  (let [tx (create-task tx-data)]
    (ds/transact! conn tx)))

(defn complete-tasks-db [db tx-data]
  (->> tx-data
     (q/get-tasks-by-ids-db db)
     (map #(update % :task/completed not))))

(defn complete-tasks [{::db/keys [conn]} tx-data]
  (let [db (ds/db conn)]
    (complete-tasks-db db tx-data)))

(defn complete-tasks! [{::db/keys [conn] :as sys} tx-data]
  (->> tx-data
     (complete-tasks sys)
     (ds/transact! conn)))

(defn merge-data-fn [before after]
  (distinct (for [b (sort-by :db/id before)
                  a (sort-by :db/id after)]
              (merge b a))))

(defn update-tasks-db [db tx-data]
  (let [tx-data-id (->> tx-data
                      (map #(select-keys % [:db/id])))
        actual (q/get-tasks-by-ids-db db tx-data-id)]
    (merge-data-fn actual tx-data)))

(defn update-tasks [{::db/keys [conn]} tx-data]
  (let [db (ds/db conn)]
    (update-tasks-db db tx-data)))

(defn update-tasks! [{::db/keys [conn] :as sys} tx-data]
  (->> tx-data
     (update-tasks sys)
     (ds/transact! conn)))

(defn delete-args?
  [{:keys [delete]}]
  (true? delete))

#_(defn delete-task
    [db {:keys [id]}]
    (let [parsed-id (read-string id)
          task      (find-task db parsed-id)]
      [:db.fn/retractEntity parsed-id]))

#_(defn task-handler
    [conn args]
    (cond (create-args? args) (create-task args)
          (update-args? args) (update-task (ds/db conn) args)
          (delete-args? args) (delete-task (ds/db conn) args)))

#_(defn tx-id!
    [task tx]
    (let [id-task (get task :db/id)
          id-temp (-> tx :tempids first second)]
      (if (> id-task 0)
        (assoc task :id (str id-task))
        (assoc task :id (str id-temp)))))

#_(defn define-task!
    [{:datascript/keys [conn]} args _value]
    (let [task    (task-handler conn args)
          tx-task (ds/transact! conn [task])]
      (cond (= :db.fn/retractEntity (first task)) {:id     (str (second task))
                                                   :delete true}
            :else                                 (tx-id! task tx-task))))
