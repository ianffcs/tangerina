(ns tangerina.main.atom-db)

(def state (atom {}))

(defn start-db []
  (reset! state (atom nil)))

(defn atom-impl
  [{:tangerina.main.core/keys [state]}]
  (letfn [(next-id []
            (::last-id (swap! state update ::last-id (fnil inc 0))))]
    {:query/tasks          (fn [_ _ _]
                             (vals (get @state :task/by-id)))
     :query/impl           (constantly "atom")
     :mutation/create-task (fn [_ {:keys [description]} _]
                             (let [id   (next-id)
                                   task {:id          id
                                         :checked     false
                                         :description description}]
                               (swap! state #(assoc-in % [:task/by-id id] task))
                               task))}))
