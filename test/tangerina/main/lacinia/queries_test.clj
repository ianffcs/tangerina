(ns tangerina.main.lacinia.queries-test
  (:require [clojure.walk :as walk]
            [clojure.test :refer [testing is deftest]]
            [com.wsscode.pathom.connect.graphql2 :as pg]
            [com.walmartlabs.lacinia :as lacinia]
            [tangerina.main.lacinia.schema :as lacinia.schema]
            [tangerina.main.lacinia.mutations :as mutations]
            [tangerina.main.core :as core]
            [datascript.core :as ds])
  (:import (clojure.lang IPersistentMap)))

(defn simplify
  "Converts all ordered maps nested within the map into standard hash maps, and
   sequences into vectors, which makes for easier constants in the tests, and eliminates ordering problems."
  [m]
  (walk/postwalk
   (fn [node]
     (cond
       (instance? IPersistentMap node)
       (into {} node)

       (seq? node)
       (vec node)

       :else
       node))
   m))

(defn lacinia-execute
  [schema gql variables context]
  (:data
   (simplify
    (lacinia/execute
     schema
     gql
     variables
     context))))

(deftest queries-test
  (let [init-conn (-> :dev
                     core/system-map
                     core/init-db
                     ::ds/schema
                     )]))

(deftest defineTask-test
  (let [context    (core/init-db (core/system-map :dev))
        execution  #(lacinia-execute
                     (lacinia.schema/graphql-schema)
                     %                    nil context)
        _insertion (do (execution
                        (pg/query->graphql
                         `[{(defineTask {:description "Foo"})
                            [:id :completed :description]}]
                         {}))
                       (execution
                        (pg/query->graphql
                         `[{(defineTask {:description "Bar"})
                            [:id :completed :description]}]
                         {})))]
    (testing "list a task"
      (is (= (execution
              (pg/query->graphql
               `[{(:listTasks {:id 1})
                  [:id :completed :description]}]
               {}))
             {:listTasks [{:id          "1"
                           :completed   false
                           :description "Foo"}]})))
    (testing "listing tasks"
      (is (= (execution
              (pg/query->graphql
               `[{(:listTasks {})
                  [:id :completed :description]}]
               {}))
             {:listTasks [{:id          "1"
                           :completed   false
                           :description "Foo"}
                          {:id          "2"
                           :completed   false
                           :description "Bar"}]})))))
