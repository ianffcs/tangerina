(ns tangerina.main.lacinia.mutations-test
  (:require [clojure.walk :as walk]
            [clojure.test :refer [testing is deftest]]
            [com.wsscode.pathom.connect.graphql2 :as pg]
            [com.walmartlabs.lacinia :as lacinia]
            [tangerina.main.lacinia.schema :as lacinia-schema]
            [tangerina.main.datascript.schema :as ds-schema]
            [tangerina.main.core :as core]
            [tangerina.main.datascript.core :as db])
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
  [schema eql variables context]
  (let [gql (pg/query->graphql eql {})]
    (:data
     (simplify
      (lacinia/execute
       schema
       gql
       variables
       context)))))

(def task-response
  [:task/id :task/description :task/completed])

(defn create-task-mutation [description]
  `[{(createTask {:task/description ~description})
     ~task-response}])

(defn get-task-query [id]
  `[{(:getTask {:db/id ~id})
     ~task-response}])

(def test-system
  {::ds-schema/schema      (ds-schema/schema)
   ::lacinia-schema/schema (lacinia-schema/graphql-schema)})

#_(deftest queries-test
    (let [server-atom                      (atom nil)
          server                           (core/prep-server server-atom test-system)
          {::lacinia-schema/keys [schema]} @server
          to-create                        [(create-task-mutation "oi")
                                            (create-task-mutation "ola")
                                            (create-task-mutation "aeo")]]
      (swap! server db/start-db!)

      (let [execution #(lacinia-execute schema % nil @server)]
        (reduce (fn [acc v] (conj acc
                                 (execution (create-task-mutation v))))
                [] to-create))

      (swap! server db/stop-db!)
      ))

#_(deftest defineTask-test
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
