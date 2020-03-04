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

(defn complete-tasks!
  [task-cursor]
  (async/go
    (async/<! (gql/defineTask!
                {:http-driver http/request-async}
                (update @task-cursor
                        :completed not)))
    (swap! task-cursor update :completed not)))

(defn get-all-tasks! [tasks]
  (async/go
    (let [tasks-return (async/<!
                        (gql/listTasks! {:http-driver http/request-async} {}))]

      (reset! tasks (vec (sort-by (comp int :id) tasks-return))))))

(defn update-description
  [task-cursor]
  (let [description (:description @task-cursor)
        form-state  (r/atom description)]
    [:span {:on-click #(prn @form-state)}
     @form-state
     [:input {:type "text"}]]))

(defn task-template
  [task-cursor]
  (prn @task-cursor)
  [:<> "ID: " (:id @task-cursor) " "
   (update-description task-cursor)])

(defn task-element
  [task-cursor]
  [:div {:key (:id @task-cursor)}
   (let [checkbox (r/atom false)]
     (if (:completed @task-cursor)
       [:div [:strike (task-template task-cursor) [:input {:type      "checkbox"
                                                           :value     true
                                                           :checked   (if @checkbox "on" "off")
                                                           :on-click  #(swap! checkbox not)
                                                           :on-change #(complete-tasks! task-cursor)}]]]
       [:div (task-template task-cursor) [:input {:type      "checkbox"
                                                  :value     true
                                                  :on-click  #(swap! checkbox not)
                                                  :on-change #(complete-tasks! task-cursor)}]]))])

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
      [:label {:for "insert-task"} "Description:"]
      [:input {:id        "insert-task"
               :type      "text"
               :name      "description"
               :on-change #(reset! description (.-value (.-target %)))}]
      [:input {:type     "button"
               :value    "insert task!"
               :on-click #(insert-tasks! tasks @description)}]]]))

#_(defn insert-task-button! [description]
    [:input {:type     "text"
             :value    @description
             :on-click #(insert-tasks! description)
             :key      (:id @task-cursor)}
     (if (:completed @task-cursor)
       [:strike (task-template task-cursor)]
       (task-template task-cursor))])

(defn main
  []
  (prn :ok)
  (r/render
   [:div
    [task-list! (r/cursor state [:tasks])]
    [description-component  (r/cursor state [:tasks])]]
   (.getElementById js/document "app")))

(defn after-load
  []
  (main))
