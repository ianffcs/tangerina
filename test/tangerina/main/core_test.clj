(ns tangerina.main.core-test
  (:require [tangerina.main.core :as core]
            [clj-http.client :as client]
            [com.wsscode.pathom.connect.graphql2 :as pg]
            [cheshire.core :as json]
            [clojure.test :refer [testing is deftest use-fixtures]]))



(defn http-fixture [f]
  (core/start-server! (core/system-map :dev))
  (f)
  (core/stop-server!))

(use-fixtures :once http-fixture)

(deftest web-server
  (testing "rest route"
    (is (= (select-keys (client/get "http://localhost:8888/greet")
                        [:body :status])
           {:body   "Hello, world!"
            :status 200})))
  (testing "graphql route"
    (is (= (select-keys (client/post "http://localhost:8888/graphql"
                                     {:content-type :graphql
                                      :body         (pg/query->graphql
                                                     `{:hello []} {})})
                        [:body :status])
           {:body   "{\"data\":{\"hello\":\"Hello, Clojurians!\"}}"
            :status 200}))))

(deftest graphql-crud-test
  (testing "create task"
    (is (= (select-keys
            (client/post "http://localhost:8888/graphql"
                         {:content-type :graphql
                          :body         (pg/query->graphql
                                         `[{(defineTask
                                              {:id          666
                                               :description "Foo"})
                                            [:description]}]
                                         {})
                          })
            [:body :status])
           {:body   "{\"data\":{\"createTask\":{\"description\":\"Foo\"}}}"
            :status 200})))
  (testing "list tasks"
    (is (= (select-keys
            (client/post "http://localhost:8888/graphql"
                         {:content-type :graphql
                          :body         (pg/query->graphql
                                         `[{(:listTasks {:id 666})
                                            [:description]}]
                                         {})
                          })
            [:body :status])
           {:body   "{\"data\":{\"listTasks\":{\"description\":\"Foo\"}}}"
            :status 200}))))
