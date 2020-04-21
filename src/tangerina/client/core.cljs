(ns tangerina.client.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
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
  (let [checkbox       (r/atom false)
        completed?     (:completed @task-cursor)
        checking       (if completed? "on" "off")
        check-checkbox #(swap! checkbox not)
        on-completion  #(complete-task! task-cursor)
        input-props    {:type      "checkbox"
                        :value     true
                        :checked   checking
                        :on-click  check-checkbox
                        :on-change on-completion}]
    (if completed?
      [:span {:class "taskCompletion"}
       [:input input-props]]
      [:span {:class "taskCompletion"}
       [:input (dissoc input-props :checked)]])))

(defn ui-pre-edit-form [{::keys [description
                                 begin-edit]}]
  [:span {:on-click begin-edit} description])

(defn ui-editing-form [{::keys [description
                                on-edition
                                close-edition]}]
  [:span [:input {:type         "text"
                  :value        description
                  :on-change    on-edition
                  :on-key-press close-edition}]])

(defn update-description
  [task-cursor]
  (let [description   (:description @task-cursor)
        editing?      (:editing @task-cursor)
        on-edition    #(swap! task-cursor assoc :description (.. % -target -value))
        begin-edit    #(swap! task-cursor assoc :editing true)
        close-edition #((when (= 13 (.-charCode %))
                          (update-task! task-cursor)))]
    (if editing?
      [:span {:class "taskDescription"
              :style {:margin "3px"}}
       [ui-editing-form {::description   description
                         ::on-edition    on-edition
                         ::close-edition close-edition}]]
      [:span {:class "taskDescription"
              :style {:margin "3px"}}
       [ui-pre-edit-form {::description description
                          ::begin-edit  begin-edit}]])))

(defn delete-task
  [task-cursor]
  (let [delete      (:delete @task-cursor)
        on-deletion #(delete-task! task-cursor)]
    (if delete
      []
      [:span {:class "taskDeletion"}
       [:input {:type     "button"
                :value    "❌"
                :on-click on-deletion}]])))

(defn ui-id [task-cursor]
  [:span {:class "taskId"
          :style {:background-color "rgba(120, 50, 50, 0.63)"
                  :border-radius    "50px"
                  :width            "30px"
                  :display          "inline-block"
                  :margin           "3px"
                  :padding          "3px 2px"
                  :text-align       "center"}}
   (:id @task-cursor)])

(defn task-template
  [task-cursor]
  (let [delete (:delete @task-cursor)]
    (if delete
      [:<>]
      [:<>
       [complete-task task-cursor]
       [ui-id task-cursor]       " "
       [update-description task-cursor]
       [delete-task task-cursor]])))

(defn task-element
  [task-cursor]
  (let [id         (:id @task-cursor)
        completed? (:completed @task-cursor)]
    [:div {:key id}
     (if completed?
       [:div [:strike (task-template task-cursor)]]
       [:div (task-template task-cursor)])]))

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

<<<<<<< Updated upstream
(defn ui-description-component [{::keys [on-description-text
                                         on-change
                                         on-submit]}]
  [:div
   [:form {:action ""
           :method :post
           :ref    on-description-text}
    [:label {:for "insertTask"} "Description:"]
    [:input {:id        "insertTask"
             :type      "text"
             :name      "description"
             :style     {:height "2px"}
             :on-change on-change}]
    [:input {:type     "button"
             :value    "insert task!"
             :on-click on-submit}]]])

(defn description-component [tasks]
  (let [description         (r/atom "")
        on-description-text #(swap! description str)
        on-change           #(reset! description (.-value (.-target %)))
        on-submit           #(insert-tasks! tasks @description)]
    [:div [ui-description-component {::on-description-text on-description-text
                                     ::on-change           on-change
                                     ::on-submit           on-submit}]]))

(defn ui-description-component
  [{::keys [on-description
            description
            on-submit]}]
  [:form {:on-submit #(do
                        (.preventDefault %)
                        (when on-submit
                          (on-submit %)))}
   [:label
    "Description:"
    [:input {:value    description
             :onChange #(on-description (.-value (.-target %)))}]
    [:input {:disabled (not (fn? on-submit))
             :value    "Input Task!"
             :type     "submit"}]]])


(defn description-component
  [{::keys [tasks]}]
  (let [description (r/atom "")]
    (fn [{::keys [tasks]}]
      (let [current-description @description
            on-description #(reset! description %)
            on-submit (when-not (string/blank? current-description)
                        #(do
                           (insert-tasks! tasks current-description)
                           (on-description "")))]
        [ui-description-component {::on-description on-description
                                   ::description    current-description
                                   ::on-submit      on-submit}]))))
(defn index
  []
  [:<>
   [task-list! (r/cursor state [:tasks])]
   [description-component  (r/cursor state [:tasks])]])

(defn main
  []
  (dom/render
   [index]
   (.getElementById js/document "app")))

(defn after-load
  []
  (main))
