(ns tangerina.main.lacinia.mutations-test
  (:require [clojure.walk :as walk]
            [clojure.test :refer [testing is deftest]]
            [com.wsscode.pathom.connect.graphql2 :as pg]
            [com.walmartlabs.lacinia :as lacinia]
            [tangerina.main.lacinia.schema :as lacinia.schema]
            [tangerina.main.core :as core])
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

(deftest defineTask-test
  (let [context   (core/init-db (core/system-map :dev))
        execution #(lacinia-execute
                    (lacinia.schema/graphql-schema)
                    %                    nil context)]
    (testing "create task"
      (is (= (execution
              (pg/query->graphql
               `[{(defineTask {:description "Foo"})
                  [:id :completed :description]}]
               {}))
             {:defineTask {:id          "1"
                           :description "Foo"
                           :completed   false}})))
    (testing "updating task"
      (is (= (execution
              (pg/query->graphql
               `[{(defineTask {:id          1
                               :description "Bar"
                               :completed   true})
                  [:id :completed :description]}]
               {}))
             {:defineTask {:id          "1"
                           :description "Bar"
                           :completed   true}})))))
