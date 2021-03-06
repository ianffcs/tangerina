(ns tangerina.client.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [tangerina.client.request-graphql :as gql]
            [clojure.string :as string]))

(def state (r/atom {:tasks :state/pending}))

(defn check-task [task-cursor]
  (let [checkbox       (r/atom false)
        completed?     (:checked @task-cursor)
        checking       (if completed? "on" "off")
        check-checkbox #(swap! checkbox not)
        on-completion  #(gql/check-task! task-cursor)
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
  [:form {:on-submit #(do
                        (.preventDefault %)
                        (when close-edition
                          (close-edition %)))}
   [:input {:type      "text"
            :value     description
            :on-change on-edition}]])

(defn set-description
  [task-cursor]
  (let [description   (:description @task-cursor)
        editing?      (:editing @task-cursor)
        on-edition    #(swap! task-cursor assoc :description (.. % -target -value))
        begin-edit    #(swap! task-cursor assoc :editing true)
        close-edition #(when-not (string/blank? description)
                         (gql/set-description-task! task-cursor))]
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
        on-deletion #(gql/delete-task! task-cursor)]
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
       [check-task task-cursor]
       [ui-id task-cursor]       " "
       [set-description task-cursor]
       [delete-task task-cursor]])))

(defn task-element
  [task-cursor]
  (let [id         (:id @task-cursor)
        completed? (:checked @task-cursor)]
    [:div {:key id}
     (if completed?
       [:div [:strike (task-template task-cursor)]]
       [:div (task-template task-cursor)])]))

(defn tasks-loading? [tasks]
  (= :state/pending @tasks))

(defn ui-tasks-list
  [tasks]
  (if (tasks-loading? tasks)
    (do
      (gql/get-all-tasks! tasks)
      "loading")
    (doall (map
            #(task-element (r/cursor tasks [%])) (range (count @tasks))))))

(defn task-list!
  [tasks]
  [:div
   (ui-tasks-list tasks)])

(defn ui-description-component
  [{::keys [on-description
            description
            on-submit]}]
  [:form {:on-submit #(do (.preventDefault %)
                          (when on-submit
                            (on-submit %)))}
   [:label
    "Description:"
    [:input {:value    description
             :onChange #(on-description (.-value (.-target %)))}]
    [:input {:disabled (not (fn? on-submit))
             :type     "submit"}]]])

(defn description-component
  [{::keys [tasks]}]
  (let [description (r/atom "")]
    (fn [{::keys [tasks]}]
      (let [current-description @description
            on-description      #(reset! description %)
            on-submit           (when-not (string/blank? current-description)
                                  #(do (gql/insert-tasks! tasks current-description)
                                       (on-description "")))]
        [ui-description-component {::on-description on-description
                                   ::description    current-description
                                   ::on-submit      on-submit}]))))

(defn index
  []
  [:<>
   [task-list! (r/cursor state [:tasks])]
   [description-component {::tasks (r/cursor state [:tasks])}]])

(defn main
  []
  (.log js/console "b")
  (dom/render
   [index]
   (.getElementById js/document "app")))

(defn ^:dev/after-load after-load
  []
  (main))
