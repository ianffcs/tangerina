(ns tangerina.main.datascript.transactions)

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

(defn update-tasks
  [tx-data-actual tx-data-after]
  (when-not (and (empty? tx-data-after) (empty? tx-data-actual))
    (distinct (for [ac (->> tx-data-actual
                          (remove nil?)
                          (sort-by :db/id))
                    af (sort-by :db/id tx-data-after)]
                (merge ac af)))))

(defn delete-tasks
  [tx-data data-db]
  (when-not (empty? data-db)
    (->> tx-data
       (remove nil?)
       (reduce (fn [acc v]
                 (conj acc [:db.fn/retractEntity (:db/id v)])) []))))
