(ns tangerina.main.core-test
  (:require [clj-http.client :as client]
            [matcher-combinators.test :refer [match?]]
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

(deftest api-test
  (let [env (-> (core/create-system {::core/conn                  (ds/create-conn tg-ds/schema)
                                    ::core/state                 (adb/start-db)
                                    ::core/lacinia-pedestal-conf {:api-path "/graphql"
                                                                  :ide-path "/graphiql"}})
               (->test-system))]
    (testing
        "Create a task in ds-impl"
      (is (= {:data {:createTask {:description "ds"}}}
             (gql env ::core/ds-http-service `[{(createTask {:description "ds"})
                                                [:description]}]))))
    (testing
        "Fetch tasks from ds-impl"
      (is (= {:data {:impl  "datascript"
                     :tasks [{:checked     false
                              :description "ds"
                              :id          1}]}}
             (gql env ::core/ds-http-service `[{:tasks [:description :checked :id]}
                                               :impl]))))
    (prn (gql env ::core/ds-http-service `[{:tasks [:description :checked :id]}
                                           :impl]))
    #_#_(testing
            "create a task in atom-impl"
          (is (= {:data {:createTask {:description "atom"}}}
                 (gql env ::core/atom-http-service `[{(create_task {:description "atom"})
                                                      [:description]}]))))

    (testing
        "Fetch from both"
      (is (= {:data {:impl  "datascript+atom"
                     :tasks [{:checked     false
                              :description "ds"}
                             {:checked     false
                              :description "atom"}]}}
             (gql env ::core/lacinia-wtf-service `[{:tasks [:description :checked]}
                                                   :impl]))))))

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

#_(deftest web-server
    (testing "graphql route"
      (let [test-atom (atom nil)
            _start    (->> {::core/conn                  (ds/create-conn tg-ds/schema)
                            ::core/state                 adb/state
                            ::core/lacinia-pedestal-conf {:api-path "/graphql"
                                                          :ide-path "/graphiql"}}
                           core/create-system
                           (core/start-system! test-atom))]
        (is (match? {:body   "{\"data\":{\"create_task\":{\"description\":\"atom\"}}}",
                     :status 200}
                    (http-gql `[{(create_task {:description "atom"})
                                 [:description]}])))
        (core/stop-system @test-atom))))
