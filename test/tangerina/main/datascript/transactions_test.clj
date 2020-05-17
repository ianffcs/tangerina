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

(def test-system
  {::ds-schema/schema (ds-schema/schema)})

(deftest create-task-test
  (let [server-atom (atom nil)
        server (core/prep-server server-atom test-system)]
    (swap! server db/start-db!)

    (let [task-1 [{:task/description "oi"}]]
      (testing "before"
        (is (empty? (q/get-all-tasks @server))))

      (let [create-1 (tx/create-task! @server task-1)
            db-1 (get create-1 :db-after)
            created-1 {:db/id 1
                       :task/description "oi"
                       :task/completed false}]
        (testing "create a task"
          (is (= 1 (count (q/get-all-tasks @server))))
          (is (= created-1 (q/get-task-by-id-db db-1 1)))
          (is (= created-1
                 (q/get-task-by-id @server 1))))))

    (swap! server db/stop-db!)))

(deftest get-tasks-by-ids-test
  (let [server-atom (atom nil)
        server (core/prep-server server-atom test-system)
        tasks [{:task/description "oi"}
               {:task/description "ola"}
               {:task/description "aeo"}]]

    (swap! server db/start-db!)

    (let [create-tasks (tx/create-task! @server tasks)
          db-tasks (get create-tasks :db-after)]

      (is (= 3 (count (q/get-all-tasks @server))))
      (is (= [{:db/id 1, :task/description "oi", :task/completed false}
              {:db/id 2, :task/description "ola", :task/completed false}
              {:db/id 3, :task/description "aeo", :task/completed false}]
             (q/get-tasks-by-ids @server (map
                                          #(assoc {} :db/id %)
                                          [1 2 3]))
             (sort-by :db/id (q/get-all-tasks-db db-tasks)))))

    (swap! server db/stop-db!)))

(deftest completing-tasks
  (let [server-atom (atom nil)
        server (core/prep-server server-atom test-system)
        tasks [{:task/description "oi"}
               {:task/description "ola"}
               {:task/description "aeo"}]
        tasks-id (map #(assoc {} :db/id %) [1 2 3])]
    (swap! server db/start-db!)

    (let [create-tasks (tx/create-task! @server tasks)
          db-tasks (get create-tasks :db-after)]

      (testing "pure function"
        (is (= [{:db/id 1, :task/description "oi", :task/completed true}
                {:db/id 2, :task/description "ola", :task/completed true}
                {:db/id 3, :task/description "aeo", :task/completed true}]
               (tx/complete-tasks-db db-tasks tasks-id))))

      (testing "execution"
        (let [completed (tx/complete-tasks! @server tasks-id)
              db-completed (get completed :db-after)]
          (is (every? true? (map :task/completed
                                 (q/get-all-tasks-db db-completed)))))))

    (swap! server db/stop-db!)))

(deftest updating-tasks
  (let [server-atom (atom nil)
        server (core/prep-server server-atom test-system)
        tasks [{:task/description "oi"}
               {:task/description "ola"}
               {:task/description "aeo"}]
        tasks-up [{:db/id 1 :task/description "abc"}
                  {:db/id 2 :task/description "consegui?"}]]
    (swap! server db/start-db!)

    (testing "not updating if not created"
      (is (nil? (tx/update-tasks @server tasks-up))))

    (let [create-tasks (tx/create-task! @server tasks)
          db-tasks (get create-tasks :db-after)]

      (testing "pure function"
        (is (= [{:db/id 1 :task/description "abc" :task/completed false}
                {:db/id 2 :task/description "consegui?" :task/completed false}]
               (tx/update-task-db db-tasks tasks-up))))

      (testing "execution"
        (let [updated (tx/update-tasks! @server tasks-up)
              db-update (get updated :db-after)]
          (is (= [{:db/id 1, :task/description "abc", :task/completed false}
                  {:db/id 2, :task/description "consegui?", :task/completed false}
                  {:db/id 3, :task/description "aeo", :task/completed false}]
                 (sort-by #(get % :db/id) (q/get-all-tasks-db db-update)))))))

    (swap! server db/stop-db!)))

#_(deftest deleting-tasks
    (let [server-atom (atom nil)
          server (core/prep-server server-atom test-system)
          tasks [{:task/description "oi"}
                 {:task/description "ola"}
                 {:task/description "aeo"}]]
      (swap! server db/start-db!)

      (let [create-tasks (tx/create-task! @server tasks)
            db-tasks (get create-tasks :db-after)]

        (testing "delete pure"
          (= nil
             (tx/delete-task))
          )

        )))
