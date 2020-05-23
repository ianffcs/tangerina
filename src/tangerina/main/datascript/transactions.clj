(ns tangerina.main.datascript.transactions
  (:require [datascript.core :as ds]))

(defn create-task
  [description]
  (let [tempid (ds/tempid :db.part/user)]
    [[:db/add tempid :task/description description]
     [:db/add tempid :task/completed false]]))

(defn uncomplete-task
  [id]
  [[:db/add id :task/completed false]])

(defn complete-task
  [id]
  [[:db/add id :task/completed true]])

(defn update-task
  [id description]
  [[:db/add id :task/description description]])

(defn delete-task
  [id]
  [[:db.fn/retractEntity id]])
