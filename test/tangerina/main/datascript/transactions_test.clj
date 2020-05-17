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

(deftest create-task-test
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

(deftest get-tasks-by-ids-test
  (let [tasks [{:task/description "oi"}
               {:task/description "ola"}
               {:task/description "kct"}]
        create-tasks (tx/create-task! @test-server tasks)
        db-tasks (get create-tasks :db-after)]
    (is (= 3 (count (q/get-all-tasks @test-server))))
    (is (= [{:db/id 1, :task/description "oi", :task/completed false}
            {:db/id 2, :task/description "ola", :task/completed false}
            {:db/id 3, :task/description "kct", :task/completed false}]
           (q/get-tasks-by-ids @test-server (map
                                             #(assoc {} :db/id %)
                                             [1 2 3]))
           (sort-by :db/id (q/get-all-tasks-db db-tasks))))))

(deftest completing-tasks
  (let [tasks [{:task/description "oi"}
               {:task/description "ola"}
               {:task/description "kct"}]
        create-tasks (tx/create-task! @test-server tasks)
        db-tasks (get create-tasks :db-after)
        tasks-id (map #(assoc {} :db/id %) [1 2 3])]
    (testing "pure function"
      (is (= [{:db/id 1, :task/description "oi", :task/completed true}
              {:db/id 2, :task/description "ola", :task/completed true}
              {:db/id 3, :task/description "kct", :task/completed true}]
             (tx/complete-tasks-db db-tasks tasks-id))))

    (testing "execution"
      (let [completed (tx/complete-tasks! @test-server tasks-id)
            db-completed (get completed :db-after)]
        (is (every? true? (map :task/completed
                               (q/get-all-tasks-db db-completed))))))))
