(ns tangerina.main.datascript.transactions-test
  (:require [clojure.test :refer [testing deftest is use-fixtures]]
            [datascript.core :as ds]
            [tangerina.main.core :as core]
            [tangerina.main.datascript.schema :as ds-schema]
            [tangerina.main.datascript.core :as db]
            [tangerina.main.datascript.queries :as q]
            [tangerina.main.datascript.transactions :as tx]))

(deftest create-task-pure
  (is (= [] (tx/create-task [])))
  (is (= [{:task/description "oi"
           :task/completed false}]
         (tx/create-task
          [{:task/description "oi"}]))))

(def test-server
  (atom nil))

(def system
  {::ds-schema/schema (ds-schema/schema)})

(defn setup-teardown-connection [f]
  (let [server (core/prep-server test-server system)]
    (swap! server db/start-db!)
    (f)
    (swap! server db/stop-db!)))

(use-fixtures :each setup-teardown-connection)

(deftest creating-tasks
  (let [task-1 [{:task/description "oi"}]]
    (testing "before"
      (is (empty? (q/get-all-tasks @test-server))))

    (let [create-1 (tx/create-task! @test-server task-1)
          db-1 (get create-1 :db-after)
          created-1 {:db/id 1
                     :task/description "oi"
                     :task/completed false}]
      (testing "create a task"
        (is (= 1 (count (q/get-all-tasks @test-server))))
        (is (= created-1 (q/get-task-by-id-db db-1 1)))
        (is (= created-1
               (q/get-task-by-id @test-server 1)))))))
