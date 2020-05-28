(ns tangerina.main.core-test
  (:require [clj-http.client :as client]
            [clojure.test :refer [testing is deftest]]
            [io.pedestal.http :as http]
            [io.pedestal.test :refer [response-for]]
            [clj-kondo.core :as kondo]
            [clojure.data.json :as json]
            [com.wsscode.pathom.connect.graphql2 :as pcgql]
            [datascript.core :as ds]
            [tangerina.main.core :as core]
            [tangerina.main.atom-db :as adb]
            [tangerina.main.datascript :as tg-ds]))

(defonce last-app (atom nil))

(defn ->test-system
  [{::core/keys [http-services]
    :as         env}]
  (swap! last-app (fn [last-env]
                    (core/stop-system last-env)
                    (core/start-system env)))
  (reduce
   (fn [acc k]
     (update acc k #(-> %
                       http/create-servlet)))
   env http-services))

(defn gql
  [env service-name eql]
  (-> eql
     (pcgql/query->graphql {})
     (->> (hash-map :query))
     json/write-str
     (->> (response-for (get-in env [service-name ::http/service-fn])
                      :post "/graphql"
                      :headers {"Content-Type" "application/json"}
                      :body))
     :body
     (json/read-str :key-fn keyword)))

#_(pcgql/query->graphql `[{(createTask {:description "ds"})
                           [:description]}] {})
(deftest api-test
  (let [env (-> (core/create-system {::core/conn                  (ds/create-conn tg-ds/schema)
                                    ::core/state                 (adb/start-db)
                                    ::core/lacinia-pedestal-conf {:api-path "/graphql"
                                                                  :ide-path "/graphiql"}})
               (->test-system))]
    (testing
        "Create a task in ds-impl"
      (is (= {:data {:createTask {:id 1, :checked false, :description "ds"}}}
             (gql env ::core/ds-http-service `[{(createTask {:description "ds"})
                                                [:id :checked :description]}]))))
    (testing
        "Fetch tasks from ds-impl"
      (is (= {:data {:tasks [{:checked     false
                              :description "ds"
                              :id          1}]}}
             (gql env ::core/ds-http-service `[{:tasks [:description :checked :id]}]))))
    (testing "completing a task"
      (is (= {:data {:completeTask {:description "ds"
                                    :checked     true
                                    :id          1}}}
             (gql env ::core/ds-http-service `[{(completeTask {:id 1})
                                                [:description :checked :id]}]))))

    (testing "updating a task"
      (is (= {:data {:updateTask {:description "alo"
                                  :checked     true
                                  :id          1}}}
             (gql env ::core/ds-http-service `[{(updateTask {:id 1 :description "alo"})
                                                [:description :checked :id]}]))))
    (testing "uncompleting a task"
      (is (= {:data {:completeTask {:description "alo"
                                    :checked     false
                                    :id          1}}}
             (gql env ::core/ds-http-service `[{(completeTask {:id 1})
                                                [:description :checked :id]}]))))
    (testing "deleting a task"
      (is (= {:data {:deleteTask {:description "alo"
                                  :checked     false
                                  :id          1}}}
             (gql env ::core/ds-http-service `[{(deleteTask {:id 1})
                                                [:description :checked :id]}])))
      (is (= {:data {:tasks []}}
             (gql env ::core/ds-http-service `[{:tasks [:description :checked :id]}]))))))

(deftest code-quality
  (is (empty? (:findings (kondo/run! {})))))

(defn http-gql [eql]
  (-> {:url          "http://localhost:8888/graphql"
      :method       :post
      :content-type :json
      :body         (-> eql
                       (pcgql/query->graphql {})
                       (->> (hash-map :query))
                       json/write-str)}
     client/request))
