(ns tangerina.client.core
  (:require [reagent.core :as r]
            [tangerina.client.http-driver :as http]
            [tangerina.client.request-graphql :as gql]
            [clojure.core.async :as async]))

(comment (gql/defineTask!
           {:http-driver http/request-async}
           {:description "oi"}))

(def state (r/atom {:tasks :state/pending}))

(defn insert-tasks! [tasks description]
  (async/go
    (let [inserted (async/<! (gql/defineTask!
                               {:http-driver http/request-async}
                               {:description description}))]
      (swap! tasks conj inserted))))

(defn complete-task!
  [task-cursor]
  (async/go
    (async/<! (gql/defineTask!
                {:http-driver http/request-async}
                (update @task-cursor
                        :completed not)))
    (swap! task-cursor update :completed not)))

(defn update-task!
  [task-cursor]
  (let [{:keys [id description completed]} @task-cursor]
    (async/go
      (async/<! (gql/defineTask!
                  {:http-driver http/request-async}
                  {:id id :description description :completed completed}))
      (swap! task-cursor assoc :editing false)
      (swap! task-cursor dissoc :editing))))

(defn delete-task!
  [task-cursor]
  (let [{:keys [id delete]} @task-cursor]
    (async/go
      (async/<! (gql/defineTask!
                  {:http-driver http/request-async}
                  {:id id :delete true}))
      (swap! task-cursor update :delete not))))

(defn get-all-tasks! [tasks]
  (async/go
    (let [tasks-return (async/<!
                        (gql/listTasks! {:http-driver http/request-async} {}))]

      (reset! tasks (vec (sort-by (comp int :id) tasks-return))))))

(defn complete-task [task-cursor]
  (let [checkbox (r/atom false)]
    (if (:completed @task-cursor)
      [:input {:type      "checkbox"
               :value     true
               :checked   (if (:completed @task-cursor) "on" "off")
               :on-click  #(swap! checkbox not)
               :on-change #(complete-task! task-cursor)}]
      [:input {:type      "checkbox"
               :value     true
               :on-click  #(swap! checkbox not)
               :on-change #(complete-task! task-cursor)}])))

(defn update-description
  [task-cursor]
  (let [description (:description @task-cursor)]
    (if (:editing @task-cursor)
      [:span [:input {:type         "text"
                      :value        description
                      :on-change    #(swap! task-cursor assoc :description (.. % -target -value))
                      :on-key-press #((when (= 13 (.-charCode %))
                                        (update-task! task-cursor)))}]]
      [:span {:on-click #(swap! task-cursor assoc :editing true)}
       description])))

(defn delete-task
  [task-cursor]
  (let [delete (:delete @task-cursor)]
    (if delete
      [:span]
      [:input {:type     "button"
               :value    "❌"
               :on-click #(delete-task! task-cursor)}])))

(defn task-template
  [task-cursor]
  (let [delete (:delete @task-cursor)]
    (if delete
      [:<>]
      [:<>
       [:span {:class "taskCompletion"}
        (complete-task task-cursor)]
       [:span {:class "taskId"
               :style {:background-color "rgba(120, 50, 50, 0.63)"
                       :border-radius    "50px"
                       :width            "30px"
                       :display          "inline-block"
                       :margin           "3px"
                       :padding          "3px 2px"
                       :text-align       "center"}}
        (:id @task-cursor)] " "
       [:span {:class "taskDescription"
               :style {:margin "3px"}}
        (update-description task-cursor)]
       [:span {:class "taskDeletion"}
        (delete-task task-cursor)]])))

(defn task-element
  [task-cursor]
  [:div {:key (:id @task-cursor)}
   (if (:completed @task-cursor)
     [:div [:strike (task-template task-cursor)]]
     [:div (task-template task-cursor)])])

(defn task-list
  [tasks]
  (if (= :state/pending @tasks)
    (do
      (get-all-tasks! tasks)
      "loading")
    (doall (map #(task-element (r/cursor tasks [%])) (range (count @tasks))))))

(defn task-list!
  [tasks]
  [:div
   (task-list tasks)])

(defn description-component [tasks]
  (let [description (r/atom "")]
    [:div
     [:form {:action ""
             :method :post
             :ref    #(swap! description str)}
      [:label {:for "insertTask"} "Description:"]
      [:input {:id           "insertTask"
               :type         "text"
               :name         "description"
               :style        {:height "2px"}
               :on-change    #(reset! description (.-value (.-target %)))
               :on-key-press #((when (= 13 (.-charCode %))
                                 (insert-tasks! tasks @description)))}]
      [:input {:type     "button"
               :value    "insert task!"
               :on-click #(insert-tasks! tasks @description)}]]]))

(defn main
  []
  (r/render
   [:div
    [task-list! (r/cursor state [:tasks])]
    [description-component  (r/cursor state [:tasks])]]
   (.getElementById js/document "app")))

(defn after-load
  []
  (main))
