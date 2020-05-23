(ns tangerina.main.datascript.queries
  (:require [tangerina.main.datascript.core :as db]
            [datascript.core :as ds]))

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
