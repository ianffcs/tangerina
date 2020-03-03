(ns tangerina.client.core
  (:require [reagent.core :as r]
            [tangerina.client.http-driver :as http]
            [tangerina.client.request-graphql :as gql]
            [clojure.core.async :as async]))

(comment (gql/defineTask!
           {:http-driver http/request-async}
           {:description "oi"}))

(def state (r/atom {:tasks :state/pending}))

(def click-count (r/atom 0))

(defn insert-tasks! [description]
  (async/go
    (async/<! (gql/defineTask!
                {:http-driver http/request-async}
                {:description description}))))

(defn get-all-tasks! [tasks]
  (async/go
    (let [tasks-return (async/<!
                        (gql/listTasks! {:http-driver http/request-async} {}))]
      (reset! tasks tasks-return))))

(defn task-list
  [tasks]
  (if (= :state/pending @tasks)
    (do
      (get-all-tasks! tasks)
      [:div "loading"])
    [:div (sort-by (comp int second) (map #(vector :div (:id %)) @tasks))
     [:input {:type         "button" :value "Click me!"
              #_#_:on-click ( inc)}]]))

(defn main
  []
  (prn :ok)
  (r/render
   [task-list (r/cursor state [:tasks])]
   (.getElementById js/document "app")))

(defn after-load
  []
  (main))
