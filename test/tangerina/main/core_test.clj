(ns tangerina.main.core-test
  (:require #_[clj-http.client :as client]
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

#_(deftest web-server
    (testing "rest route"
      (is (= (select-keys (client/get "http://localhost:8888/greet")
                          [:body :status])y
             {:body   "Hello, world!"
              :status 200})))
    (testing "graphql route"
      (is (= (select-keys (client/post "http://localhost:8888/graphql"
                                       {:content-type :graphql
                                        :body         (pcgql/query->graphql
                                                       `{:hello []} {})})
                          [:body :status])
             {:body   "{\"data\":{\"hello\":\"Hello, Clojurians!\"}}"
              :status 200}))))

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
                      :post "/api"
                      :headers {"Content-Type" "application/json"}
                      :body))
     :body
     (json/read-str :key-fn keyword)))

(deftest api-test
  (let [env (-> (core/create-system {::core/conn  (ds/create-conn tg-ds/schema)
                                    ::core/state (adb/start-db)})
               (->test-system))]
    (testing
        "Createa a task in ds-impl"
      (is (= {:data {:create_task {:description "ds"}}}
             (gql env ::core/ds-http-service `[{(create_task {:description "ds"})
                                                [:description]}]))))
    (testing
        "create a task in atom-impl"
      (is (= {:data {:create_task {:description "atom"}}}
             (gql env ::core/atom-http-service `[{(create_task {:description "atom"})
                                                  [:description]}]))))
    (testing
        "Fetch tasks from ds-impl"
      (is (= {:data {:impl  "datascript"
                     :tasks [{:checked     false
                              :description "ds"}]}}
             (gql env ::core/ds-http-service `[{:tasks [:description :checked]}
                                               :impl]))))
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
