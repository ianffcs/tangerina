(ns tangerina.main.gql-test
  (:require [clojure.test :refer [deftest is are testing]]
            [io.pedestal.http :as http]
            [io.pedestal.test :refer [response-for]]
            [datascript.core :as ds]
            [clj-kondo.core :as kondo]
            [com.walmartlabs.lacinia.pedestal2 :as lp]
            [clojure.data.json :as json]
            [com.walmartlabs.lacinia.schema :as lacinia.schema]
            [com.walmartlabs.lacinia.util :as lacinia.util]
            [com.wsscode.pathom.connect.graphql2 :as pcgql]
            [datascript.core :as d]))

;; app.core

(defn atom-impl
  [{::keys [state]}]
  (letfn [(next-id []
            (::last-id (swap! state update ::last-id (fnil inc 0))))]
    {:query/tasks          (fn [_ _ _]
                             (vals (get @state :task/by-id)))
     :mutation/create-task (fn [_ {:keys [description]} _]
                             (let [id (next-id)
                                   task {:id          id
                                         :checked     false
                                         :description description}]
                               (swap! state #(assoc-in % [:task/by-id id] task))
                               task))}))


(defn datascrip-impl
  [{::keys [conn]}]
  {:query/tasks          (fn [_ _ _]
                           (->> (d/q '[:find ?e ?description ?checked
                                       :in $
                                       :where
                                       [?e :task/description ?description]
                                       [?e :task/checked? ?checked]]
                                     (d/db conn))
                                (map (partial zipmap [:id :description :checked]))))
   :mutation/create-task (fn [_ {:keys [description]} _]
                           (let [id (d/tempid :db.part/user)
                                 {:keys [db-after tempids]} (d/transact! conn [{:db/id            id
                                                                                :task/checked?    false
                                                                                :task/description description}])]
                             {:id          (d/resolve-tempid db-after tempids id)
                              :checked?    false
                              :description description}))})



(defn create-system
  [{::keys [] :as env}]
  (let [ds-schema {}
        conn (ds/create-conn ds-schema)
        state (atom {})
        lacinia-schema (-> {:objects   {:Task {:fields {:id          {:type 'Int}
                                                        :checked     {:type 'Boolean}
                                                        :description {:type 'String}}}}
                            :queries   {:tasks {:type    '(list Task)
                                                :resolve :query/tasks}}
                            :mutations {:create_task {:type    'Task
                                                      :args    {:description {:type 'String}}
                                                      :resolve :mutation/create-task}}}
                           (lacinia.util/attach-resolvers
                             (atom-impl {::state state})
                             #_(datascrip-impl {::conn conn}))
                           (lacinia.schema/compile))
        http-service (lp/default-service lacinia-schema {})]
    (assoc env ::conn conn
               ::state state
               ::http-service http-service)))

;; app.main

(defn start-system
  "start with (-> (create-system {}) start-system)"
  [{::keys [http-service]
    :as    env}]
  (assoc env ::http-service (-> http-service
                                http/create-server
                                http/start)))

;; app.test

(defonce last-app (atom nil))

(defn ->test-system
  [{::keys [http-service]
    :as    env}]
  (swap! last-app (fn [{::keys [http-service]}]
                    (when http-service
                      (http/stop http-service))
                    (start-system env)))
  (assoc env ::service-fn (-> http-service
                              http/create-servlet
                              ::http/service-fn)))
(defn gql
  [{::keys [service-fn]} eql]
  (-> eql
      (pcgql/query->graphql {})
      (->> (hash-map :query))
      json/write-str
      (->> (response-for service-fn :post "/api"
                         :headers {"Content-Type" "application/json"}
                         :body))
      :body
      (json/read-str :key-fn keyword)))

;; app.core-test

(deftest api-test
  (let [env (-> (create-system {})
                (->test-system))]
    (is (= (gql env `[{(create_task {:description "world"})
                       [:description]}])
           {:data {:create_task {:description "world"}}}))
    (is (= {:data {:tasks [{:checked     false
                            :description "world"}]}}
           (gql env `[{:tasks [:description :checked]}])))))

(deftest code-quality
  (is (empty? (:findings (kondo/run! {})))))
