(ns tangerina.main.datascript.transactions-test
  (:require [clojure.test :refer [testing deftest is
                                  use-fixtures]]
            [matcher-combinators.test :refer [match?]]
            [tangerina.main.test-utils :as tu]
            [tangerina.main.core :as core]
            [tangerina.main.datascript.schema :as ds-schema]
            [tangerina.main.datascript.core :as db]
            [tangerina.main.datascript.queries :as q]
            [tangerina.main.datascript.transactions :as tx]
            [datascript.core :as ds]))

(def test-system
  {::ds-schema/schema (ds-schema/schema)})

(deftest creation-tx-data
  (let [task-1 [{:task/description "oi"}
                {:task/description "ae"}
                {:task/description "aio"}]]
    (is (match? empty? (tx/create-task [])))
    (is (match? [{:task/description "oi"
                  :task/completed   false}
                 {:task/description "ae"
                  :task/completed   false}
                 {:task/description "aio"
                  :task/completed   false}]
                (tx/create-task task-1)))))

(deftest completion-tx-data
  (let [to-complete     [{:task/description "ae"
                          :task/completed   false
                          :db/id            1}
                         {:task/description "aio"
                          :task/completed   false
                          :db/id            2}]
        completed-tasks [{:task/description "ae"
                          :task/completed   true
                          :db/id            1}
                         {:task/description "aio"
                          :task/completed   true
                          :db/id            2}]]
    (is (match? empty? (tx/complete-tasks [])))
    (is (match? completed-tasks
                (tx/complete-tasks to-complete)))))

(deftest un-completion-tx-data
  (let [to-uncomplete [{:task/description "aio"
                        :task/completed   true
                        :db/id            2}]]
    (is (match? empty? (tx/uncomplete-tasks [])))
    (is (match? [{:task/description "aio"
                  :task/completed   false
                  :db/id            2}]
                (tx/uncomplete-tasks to-uncomplete)))))

(deftest update-tx-data
  (let [actual [{:db/id 1, :task/description "oi", :task/completed false}
                {:db/id 2 :task/description "ola" :task/completed false}]
        after  [{:db/id 1, :task/description "abc"}]
        result [{:db/id 1 :task/description "abc" :task/completed false}]]
    (is (match? empty? (tx/update-tasks [] [])))
    (is (match? empty? (tx/update-tasks actual [])))
    (is (match? empty? (tx/update-tasks [] after)))
    (is (match? result (tx/update-tasks actual after)))))

(deftest delete-tasks
  (let [tx-data [{:db/id 1}
                 {:db/id 2}
                 {:db/id 3}]
        data-db [{:db/id 1 :task/description "abc" :task/completed false}]]
    (is (match? empty? (tx/delete-tasks [] [])))
    (is (match? empty? (tx/delete-tasks [] data-db)))
    (is (match? empty? (tx/delete-tasks tx-data [])))
    (is (match? [[:db.fn/retractEntity 1]
                 [:db.fn/retractEntity 2]
                 [:db.fn/retractEntity 3]]
                (tx/delete-tasks tx-data data-db)))))

(use-fixtures :once (partial tu/setup-teardown-db test-system))


;; TODO transactions-test
#_(deftest transactions-task-test
(let [task-1             [{:task/description "oi"}]
      {::db/keys [conn]} @tu/test-server
      {db-1 :db-after}   (->> task-1
                              tx/create-task
                              (ds/transact! conn))
      created-1          {:db/id            1
                          :task/description "oi"
                          :task/completed   false}]

  (testing "transaction"
    (is (= created-1 (q/get-task-by-id-db db-1 1))))))

#_(deftest get-tasks-by-ids-test
(let [server-atom (atom nil)
      server      (core/prep-server server-atom test-system)
      tasks       [{:task/description "oi"}
                   {:task/description "ola"}
                   {:task/description "aeo"}]]

  (swap! server db/start-db!)

  (let [create-tasks (tx/create-task! @server tasks)
        db-tasks     (get create-tasks :db-after)]

    (is (= 3 (count (q/get-all-tasks @server))))
    (is (= [{:db/id 1, :task/description "oi", :task/completed false}
            {:db/id 2, :task/description "ola", :task/completed false}
            {:db/id 3, :task/description "aeo", :task/completed false}]
           (q/get-tasks-by-ids @server (map
                                        #(assoc {} :db/id %)
                                        [1 2 3]))
           (sort-by :db/id (q/get-all-tasks-db db-tasks)))))

  (swap! server db/stop-db!)))

#_(deftest completing-tasks
(let [server-atom (atom nil)
      server      (core/prep-server server-atom test-system)
      tasks       [{:task/description "oi"}
                   {:task/description "ola"}
                   {:task/description "aeo"}]
      tasks-id    (map #(assoc {} :db/id %) [1 2 3])]
  (swap! server db/start-db!)

  (let [create-tasks (tx/create-task! @server tasks)
        db-tasks     (get create-tasks :db-after)]

    (testing "pure function"
      (is (= [{:db/id 1, :task/description "oi", :task/completed true}
              {:db/id 2, :task/description "ola", :task/completed true}
              {:db/id 3, :task/description "aeo", :task/completed true}]
             (tx/complete-tasks-db db-tasks tasks-id))))

    (testing "execution"
      (let [completed    (tx/complete-tasks! @server tasks-id)
            db-completed (get completed :db-after)]
        (is (every? true? (map :task/completed
                               (q/get-all-tasks-db db-completed)))))))

  (swap! server db/stop-db!)))

#_(deftest updating-tasks
    (let [server-atom (atom nil)
          server      (core/prep-server server-atom test-system)
          tasks       [{:task/description "oi"}
                       {:task/description "ola"}
                       {:task/description "aeo"}]
          tasks-up    [{:db/id 1 :task/description "abc"}
                       {:db/id 2 :task/description "consegui?"}]]
      (swap! server db/start-db!)

      (testing "not updating if not created"
        (is (nil? (tx/update-tasks @server tasks-up))))

      (let [create-tasks (tx/create-task! @server tasks)
            db-created   (get create-tasks :db-after)]

        (testing "pure function"
          (is (= [{:db/id 1 :task/description "abc" :task/completed false}
                  {:db/id 2 :task/description "consegui?" :task/completed false}]
                 (tx/update-tasks-db db-created tasks-up))))

        (testing "execution"
          (let [updated    (tx/update-tasks! @server tasks-up)
                db-updated (get updated :db-after)]
            (is (= [{:db/id 1, :task/description "abc", :task/completed false}
                    {:db/id 2, :task/description "consegui?", :task/completed false}
                    {:db/id 3, :task/description "aeo", :task/completed false}]
                   (sort-by #(get % :db/id) (q/get-all-tasks-db db-updated)))))))
      (swap! server db/stop-db!)))

#_(deftest deleting-tasks
    (let [server-atom (atom nil)
          server      (core/prep-server server-atom test-system)
          tasks       [{:task/description "oi"}
                       {:task/description "ola"}
                       {:task/description "aeo"}]
          tasks-id    (map #(assoc {} :db/id %) [1 2 4])]
      (swap! server db/start-db!)


      (let [create-tasks (tx/create-task! @server tasks)
            db-tasks     (get create-tasks :db-after)]

        (testing "delete pure only if exists"
          (is (= [[:db.fn/retractEntity 1]
                  [:db.fn/retractEntity 2]]
                 (tx/delete-tasks-db db-tasks tasks-id))))

        (testing "deleting execution"
          (let [deleted-tasks (tx/delete-tasks! @server tasks-id)
                db-deleted    (get deleted-tasks :db-after)]
            (is (= [{:db/id 3, :task/description "aeo", :task/completed false}]
                   (q/get-all-tasks-db db-deleted))))))))
