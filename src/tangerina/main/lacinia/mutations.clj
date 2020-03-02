(ns tangerina.main.lacinia.mutations
  (:require [datascript.core :as ds]))

(defn find-task
  ([db id]
   (->> (ds/q '[:find ?id ?d ?c
              :in $ ?id
              :where
              [?id :description ?d]
              [?id :completed ?c]] db id)
      first
      (zipmap [:id :description :completed]))))

(defn create-args?
  [{:keys [id]}]
  (nil? id))

(defn create-task
  [args]
  (-> args
     (assoc :completed false)
     (select-keys [:description :completed])
     (assoc :db/id -1)))

(defn update-args?
  [{:keys [id delete]}]
  (and (not (nil? id))
     (or (false? delete) (nil? delete))))

(defn update-task
  [db args]
  (let [{:keys [id]} args
        parsed-id    (read-string id)
        task         (find-task db parsed-id)]
    (when (not-empty task)
      (assoc (merge task
                    (assoc args :id parsed-id))
             :db/id parsed-id))))

(defn delete-args?
  [{:keys [delete]}]
  (true? delete))

(defn delete-task
  [db {:keys [id]}]
  (let [parsed-id (read-string id)
        task      (find-task db parsed-id)]
    [:db.fn/retractEntity parsed-id]))

(defn task-handler
  [conn args]
  (cond (create-args? args) (create-task args)
        (update-args? args) (update-task (ds/db conn) args)
        (delete-args? args) (delete-task (ds/db conn) args)))

(defn tx-id!
  [task tx]
  (let [id-task (get task :db/id)
        id-temp (-> tx :tempids first second)]
    (if (> id-task 0)
      (assoc task :id (str id-task))
      (assoc task :id (str id-temp)))))

(defn define-task!
  [{:datascript/keys [conn]} args _value]
  (let [task    (task-handler conn args)
        tx-task (ds/transact! conn [task])]
    (cond (= :db.fn/retractEntity (first task)) {:id     (str (second task))
                                                 :delete true}
          :else                                 (tx-id! task tx-task))))

(def define-task-edn
  `{:defineTask {:args    {:id          {:type ~'ID}
                           :description {:type ~'String}
                           :completed   {:type ~'Boolean}
                           :delete      {:type ~'Boolean}}
                 :type    :Task
                 :resolve ~define-task!}})

(def mutations-edn
  (merge define-task-edn))
