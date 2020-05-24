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
            [datascript.core :as d]
            [com.walmartlabs.lacinia :as lacinia]
            [clojure.string :as string]))

;; app.core

(defn atom-impl
  [{::keys [state]}]
  (letfn [(next-id []
            (::last-id (swap! state update ::last-id (fnil inc 0))))]
    {:query/tasks          (fn [_ _ _]
                             (vals (get @state :task/by-id)))
     :query/impl           (constantly "atom")
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
   :query/impl           (constantly "datascript")
   :mutation/create-task (fn [_ {:keys [description]} _]
                           (let [id (d/tempid :db.part/user)
                                 {:keys [db-after tempids]} (d/transact! conn [{:db/id            id
                                                                                :task/checked?    false
                                                                                :task/description description}])]
                             {:id          (d/resolve-tempid db-after tempids id)
                              :checked?    false
                              :description description}))})

(defn lacinia-impl
  [{::keys [lacinias]}]
  {:query/tasks          (fn [a _ _]
                           (for [impl lacinias
                                 task (-> (lacinia/execute impl
                                                           "{ tasks { id description checked } }"
                                                           {}
                                                           {})
                                          :data
                                          :tasks)]
                             task))

   :query/impl           (fn [_ _ _]
                           (string/join "+" (for [impl lacinias]
                                              (:impl (:data (lacinia/execute impl
                                                                             "{ impl }"
                                                                             {}
                                                                             {}))))))
   :mutation/create-task (fn [_ _ _]
                           (throw (ex-info "You can't mutate here" {})))})

(defn create-system
  [{::keys [] :as env}]
  (let [ds-schema {}
        conn (ds/create-conn ds-schema)
        state (atom {})
        lacinia-schema {:objects   {:Task {:fields {:id          {:type 'Int}
                                                    :checked     {:type 'Boolean}
                                                    :description {:type 'String}}}}
                        :queries   {:impl  {:type    'String
                                            :resolve :query/impl}
                                    :tasks {:type    '(list Task)
                                            :resolve :query/tasks}}
                        :mutations {:create_task {:type    'Task
                                                  :args    {:description {:type 'String}}
                                                  :resolve :mutation/create-task}}}
        ds-gql-schema (-> lacinia-schema
                          (lacinia.util/attach-resolvers (datascrip-impl {::conn conn}))
                          lacinia.schema/compile)
        atom-http-schema (-> lacinia-schema
                             (lacinia.util/attach-resolvers (atom-impl {::state state}))
                             lacinia.schema/compile)
        lacinia-wtf-schema (-> lacinia-schema
                               (lacinia.util/attach-resolvers (lacinia-impl {::lacinias [ds-gql-schema
                                                                                         atom-http-schema]}))
                               lacinia.schema/compile)
        atom-http-service (-> atom-http-schema
                              (lp/default-service {})
                              (assoc ::http/port 8888))
        lacinia-wtf-service (-> lacinia-wtf-schema
                                (lp/default-service {})
                                (assoc ::http/port 8890))
        ds-http-service (-> ds-gql-schema
                            (lp/default-service {})
                            (assoc ::http/port 8889))]
    (assoc env ::conn conn
               ::state state
               ::http-services [::ds-http-service
                                ::lacinia-wtf-service
                                ::atom-http-service]
               ::ds-http-service ds-http-service
               ::lacinia-wtf-service lacinia-wtf-service
               ::atom-http-service atom-http-service)))

;; app.main

(defn start-system
  "start with (-> (create-system {}) start-system)"
  [{::keys [http-services]
    :as    env}]
  (reduce
    (fn [acc k]
      (update acc k #(-> %
                         http/create-server
                         http/start)))
    env http-services))


(defn stop-system
  [{::keys [http-services]
    :as    env}]
  (reduce
    (fn [acc k]
      (update acc k #(some-> %
                             http/stop)))
    env http-services))


;; app.test

(defonce last-app (atom nil))

(defn ->test-system
  [{::keys [http-services]
    :as    env}]
  (swap! last-app (fn [last-env]
                    (stop-system last-env)
                    (start-system env)))
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
                         :post "/api"
                         :headers {"Content-Type" "application/json"}
                         :body))
      :body
      (json/read-str :key-fn keyword)))

;; app.core-test

(deftest api-test
  (let [env (-> (create-system {})
                (->test-system))]
    (testing
      "Createa a task in ds-impl"
      (is (= {:data {:create_task {:description "ds"}}}
             (gql env ::ds-http-service `[{(create_task {:description "ds"})
                                           [:description]}]))))
    (testing
      "create a task in atom-impl"
      (is (= {:data {:create_task {:description "atom"}}}
             (gql env ::atom-http-service `[{(create_task {:description "atom"})
                                             [:description]}]))))
    (testing
      "Fetch tasks from ds-impl"
      (is (= {:data {:tasks [{:checked     false
                              :description "ds"}]}}
             (gql env ::ds-http-service `[{:tasks [:description :checked]}]))))
    (testing
      "Fetch from both"
      (is (= {:data {:tasks [{:checked     false
                              :description "ds"}
                             {:checked     false
                              :description "atom"}]}}
             (gql env ::lacinia-wtf-service `[{:tasks [:description :checked]}]))))))

(deftest code-quality
  (is (empty? (:findings (kondo/run! {})))))
