(ns tangerina.client.request-graphql
  (:require [cljs.core.async :as async]
            ["graphql-request" :as gqlr]
            [com.wsscode.pathom.connect.graphql2 :as pcgql]))

(def graphql-app
  {:url "http://localhost:8888/graphql"})

(defn gql-request! [url eql]
  (let [chan    (async/promise-chan)
        raise   (fn request-async-raise [ex]
                  (async/put! chan (if (nil? ex)
                                     ::nil
                                     ex)))
        respond (fn request-async-respond [response]
                  (try
                    (async/put! chan response)
                    (catch #?(:clj  Throwable
                              :cljs :default) e
                      (raise e))))]
    (-> (gqlr/request url
                      (pcgql/query->graphql eql
                                            {}))
        (.then (fn [response]
                 (respond (js->clj response :keywordize-keys true))))
        (.catch (fn [err]
                  (raise err))))
    chan))

(defn execute! [eql]
  (async/go
    (->> (gql-request! (get graphql-app :url) eql)
         async/<!)))

(defn insert-tasks! [tasks description]
  (async/go
    (let [inserted (->> `[{(createTask {:description ~description})
                         [:id :checked :description]}]
                      execute!
                      async/<!
                      :createTask)]
      (swap! tasks conj inserted))))

(defn check-task!
  [task-cursor]
  (let [{:keys [id]} @task-cursor]
    (async/go
      (->> `[{(checkTask {:id ~id})
              [:id :checked :description]}]
           execute!
           async/<!
           :completeTask)
      (swap! task-cursor update :checked not))))

(defn set-description-task!
  [task-cursor]
  (let [{:keys [id description]} @task-cursor]
    (async/go
      (->> `[{(setDescriptionTask {:id ~id :description ~description})
            [:description :checked :id]}]
         execute!
         async/<!
         :setDescriptionTask)
      (swap! task-cursor assoc :editing false)
      (swap! task-cursor dissoc :editing))))

(defn delete-task!
  [task-cursor]
  (let [{:keys [id]} @task-cursor]
    (async/go
      (->> `[{(deleteTask {:id ~id})
            [:id :checked :description]}]
         execute!
         async/<!
         :deleteTask)
      (swap! task-cursor update :delete not))))

(defn get-all-tasks! [tasks]
  (async/go
    (let [tasks-return (->> `[{:tasks
                             [:id :checked :description]}]
                          execute!
                          async/<!
                          :tasks
                          (sort-by (comp int :id))
                          vec)]
      (reset! tasks tasks-return))))

#_(async/go (->> `[{(createTask {:description "foi!"})
                    [:id :checked :description]}]
                 execute!
                 async/<!
                 prn))
#_(async/go
    (->> `[{(setDescriptionTask {:id 1 :description "aaa"})
          [:description :checked :id]}]
       execute!
       async/<!
       :setDescriptionTask
       prn))

#_(async/go (->> [{:tasks
                   [:id :checked :description]}]
                 execute!
                 async/<!
                 :tasks
                 (sort-by :id)
                 prn))
