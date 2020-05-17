(ns tangerina.main.lacinia.mutations
  (:require [tangerina.main.datascript.queries :as q]
            [tangerina.main.datascript.transactions :as tx]))

(defn ->tasks-unnamed [tasks]
  (reduce (fn [acc {:db/keys   [id]
                   :task/keys [description
                               completed]}]
            (conj acc {:db/id            id
                       :task/description description
                       :task/completed   completed}))
          [] tasks))

(defn create-task!
  [system args _value]
  (prn (-> args
          (select-keys [:description])
          :description))
  (->> {:task/description (-> args
                           (select-keys [:description])
                           :description)}
     #_(q/get-task-by-id system)
     #_->tasks-unnamed))

(def create-task-edn
  `{:createTask {:args    {:description {:type ~'String}}
                 :type    :Task
                 :resolve ~create-task!}})

#_(def mutations-edn
    (merge define-task-edn))
