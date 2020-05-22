(ns tangerina.main.gql-test
  (:require [clojure.test :refer [deftest is are testing]]
            [io.pedestal.http :as http]
            [io.pedestal.test :refer [response-for]]
            [datascript.core :as ds]
            [com.walmartlabs.lacinia :as lacinia]
            [clojure.data.json :as json]
            [com.walmartlabs.lacinia.schema :as lacinia.schema]
            [com.wsscode.pathom.connect.graphql2 :as pcgql]
            [datascript.core :as d]))


(defn create-system
  [{::keys [] :as env}]
  (let [ds-schema {}
        conn (ds/create-conn ds-schema)
        lacinia-schema (-> {:objects   {:Task {:fields {:id          {:type 'Int}
                                                        :checked     {:type 'Boolean}
                                                        :description {:type 'String}}}}
                            :queries   {:tasks {:type    '(list Task)
                                                :resolve (fn [_ _ _]
                                                           (->> (d/q '[:find ?e ?description ?checked
                                                                       :in $
                                                                       :where
                                                                       [?e :task/description ?description]
                                                                       [?e :task/checked? ?checked]]
                                                                     (d/db conn))
                                                                (map (partial zipmap [:id :description :checked]))))}}
                            :mutations {:create_task {:type    'Task
                                                      :args    {:description {:type 'String}}
                                                      :resolve (fn [_ {:keys [description]} _]
                                                                 (let [id (d/tempid :db.part/user)
                                                                       {:keys [db-after tempids]} (d/transact! conn [{:db/id            id
                                                                                                                      :task/checked?    false
                                                                                                                      :task/description description}])]
                                                                   {:id            (d/resolve-tempid db-after tempids id)
                                                                    :task/checked? false
                                                                    :description   description}))}}}
                           lacinia.schema/compile)
        http-service (-> {::http/routes #{["/graphql" :post
                                           [{:name  ::error
                                             :error (fn [ctx ex]
                                                      (.printStackTrace ex)
                                                      (assoc ctx :response {:body   (json/write-str {:ex-message (ex-message ex)})
                                                                            :status 400}))}
                                            (fn [{:keys [body] :as req}]
                                              (let [{:keys [query variables]} (json/read-str (slurp body)
                                                                                             :key-fn keyword)
                                                    result (lacinia/execute lacinia-schema query variables req)]
                                                {:body   (json/write-str result)
                                                 :status 200}))]
                                           :route-name ::graphql]}}
                         http/default-interceptors)]
    (assoc env ::conn conn
               ::http-service http-service)))

(defn ->test-system
  [{::keys [http-service]
    :as    env}]
  (assoc env ::service-fn (-> http-service
                              http/create-servlet
                              ::http/service-fn)))

(defn start-system
  "start with (-> (create-system {}) start-system)"
  [{::keys [http-service]
    :as    env}]
  (assoc env ::service-fn (-> http-service
                              (assoc ::http/type :jetty
                                     ::http/join? false
                                     ::http/port 8080)
                              http/create-servlet
                              ::http/service-fn)))

(defn gql
  [{::keys [service-fn]} eql]
  (-> eql
      (pcgql/query->graphql {})
      (->> (hash-map :query))
      json/write-str
      (->> (response-for service-fn :post "/graphql"
                         :body))
      :body
      (json/read-str :key-fn keyword)))

(deftest api-test
  (let [env (-> (create-system {})
                (->test-system))]
    (is (= (gql env `[{(create_task {:description "world"})
                       [:description]}])
           {:data {:create_task {:description "world"}}}))
    (is (= (gql env `[{:tasks [:description :checked]}])
           {:data {:tasks [{:checked     false
                            :description "world"}]}}))))
