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
        actual (->> (q/get-tasks-by-ids-db db tx-data-id)
                  (remove nil?))]
    (when-not (empty? actual)
      (merge-data-fn actual tx-data))))

(defn update-tasks [{::db/keys [conn]} tx-data]
  (let [db (ds/db conn)]
    (update-tasks-db db tx-data)))

(defn update-tasks! [{::db/keys [conn] :as sys} tx-data]
  (->> tx-data
     (update-tasks sys)
     (ds/transact! conn)))

(defn delete-tasks-db
  [db tx-data]
  (->> tx-data
     (q/get-tasks-by-ids-db db)
     (remove nil?)
     (reduce (fn [acc v]
               (conj acc [:db.fn/retractEntity (:db/id v)])) [])))

(defn delete-tasks [{::db/keys [conn]} tx-data]
  (let [db (ds/db conn)]
    (delete-tasks-db db tx-data)))

(defn delete-tasks! [{::db/keys [conn] :as sys} tx-data]
  (->> tx-data
     (delete-tasks sys)
     (ds/transact! conn)))
