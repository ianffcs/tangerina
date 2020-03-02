(ns tangerina.main.lacinia.mutations
  (:require [datascript.core :as ds]
            [medley.core :as medley]
            [datascript.core :as d]))

(defn tx-task!->id
  [tx]
  (-> tx :tempids ffirst))

(defn find-task [db id]
  (ds/q '[:find ?d ?c
          :in $ ?id
          :where
          [?id :task/description ?d]
          [?id :task/completion ?c]] db id))

(defn create-args?
  [{:keys [id]}]
  (nil? id))

(defn create-task
  [args]
  (->> args
     (medley/map-keys #(keyword "task" (name %)))
     (select-keys [:id :description :completed])))

(defn update-args?
  [{:keys [id]}]
  (not (nil? id)))

(defn update-task
  [db args]
  (merge (find-task db (get args :id)) (create-task args)))

(defn delete-args?
  [{:keys [delete]}]
  (true? delete))

(defn delete-task
  [args]
  {:db/retract (get args :id)})

(defn task-handler
  [conn args]
  (cond (create-args? args) (create-task args)
        (update-args? args) (update-task (ds/db conn) args)
        (delete-args? args) (delete-task args)))

(defn define-task!
  [{:datascript/keys [conn]} args _value]
  (let [task (task-handler conn args)]
    (ds/transact! conn task)
    task))

(def define-task-edn
  `{:defineTask {:args    {:description {:type ~'String}
                           :completion  {:type ~'Boolean}}
                 :type    :Task
                 :resolve ~define-task!}})

(def mutations-edn
  (merge define-task-edn))
