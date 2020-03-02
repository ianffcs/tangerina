(ns tangerina.main.lacinia.mutations
  (:require [datascript.core :as ds]
            [medley.core :as medley]
            [datascript.core :as d]))

(defn tx-task!->id
  [tx]
  (-> tx :tempids ffirst))

(defn find-task
  ([db]
   (ds/q '[:find ?id ?d ?c
           :where
           [?id :task/description ?d]
           [?id :task/completed ?c]] db))
  ([db id]
   (ds/q '[:find ?id ?d ?c
           :in $ ?id
           :where
           [?id :task/description ?d]
           [?id :task/completed ?c]] db id)))

(defn create-args?
  [{:keys [id]}]
  (nil? id))

(defn create-task
  [args]
  (->  (medley/map-keys #(keyword "task" (name %)) args)
      (assoc :task/completed false)
      (select-keys [:task/id :task/description :task/completed])))

(defn update-args?
  [{:keys [id]}]
  (not (nil? id)))

(defn update-task
  [db args]
  ;; (find-task db (get args :id)) tem que ser a ultima task, serializo e depois exponho
  (merge (medley/map-keys #(keyword (name %)) (find-task db (get args :id)))
         (create-task args)))

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
  (let [task    (task-handler conn args)
        tx-task (ds/transact! conn [task])]
    (-> (medley/map-keys #(keyword (name %)) task)
       (assoc :id (str (tx-task!->id tx-task))))))

(def define-task-edn
  `{:defineTask {:args    {:id          {:type ~'ID}
                           :description {:type ~'String}
                           :completed   {:type ~'Boolean}
                           :delete      {:type ~'Boolean}}
                 :type    :Task
                 :resolve ~define-task!}})

(def mutations-edn
  (merge define-task-edn))
