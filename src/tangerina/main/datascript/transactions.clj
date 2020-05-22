(ns tangerina.main.datascript.transactions
  (:require [datascript.core :as ds]))

(defn create-tasks
  [tx-data]
  (reduce (fn [acc val]
            (conj acc (-> val
                         (select-keys [:task/description])
                         (assoc :task/completed false)))) [] tx-data))

(defn complete-tasks [tx-data]
  (->> tx-data
     (remove nil?)
     (map #(assoc % :task/completed true))))

(defn uncomplete-tasks [tx-data]
  (->> tx-data
     (remove nil?)
     (map #(assoc % :task/completed false))))

(defn update-task
  [tx-data-actual tx-data-after]
  (when (= (get tx-data-actual :db/id)
           (get tx-data-after :db/id))
    (assoc tx-data-actual
           :task/description (get tx-data-after :task/description))))

(defn update-tasks
  [txs-data-actual txs-data-after]
  (->> (update-task ac af)
     (for [ac txs-data-actual
           af txs-data-after])
     (remove nil?)))

(defn delete-tasks
  [tx-data data-db]
  (when-not (empty? data-db)
    (->> tx-data
       (remove nil?)
       (reduce (fn [acc v]
                 (conj acc [:db.fn/retractEntity (:db/id v)])) []))))

(defn uncomplete-task
  [id]
  [[:db/add id :task/completed false]])

(defn create-task
  [description]
  [[:db/add (ds/tempid :db.part/user) :task/description description]])