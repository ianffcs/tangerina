(ns tangerina.main.datascript.transactions-test
  (:require [clojure.test :refer [testing deftest is
                                  use-fixtures]]
            [matcher-combinators.test :refer [match?]]
            [tangerina.main.test-utils :as tu]
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
    (is (match? empty? (tx/create-tasks [])))
    (is (match? [{:task/description "oi"
                  :task/completed   false}
                 {:task/description "ae"
                  :task/completed   false}
                 {:task/description "aio"
                  :task/completed   false}]
                (tx/create-tasks task-1)))))

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
  (let [actual [{:db/id            2,
                 :task/description "ola",
                 :task/completed   true}
                {:db/id            3,
                 :task/description "aio",
                 :task/completed   false}]
        after  [{:db/id 2, :task/description "alo"}
                {:db/id 3, :task/description "ioa"}]
        result [{:db/id            2,
                 :task/description "alo",
                 :task/completed   true}
                {:db/id            3
                 :task/description "ioa"
                 :task/completed   false}]]
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
(deftest transactions-task-test
  (let [{::db/keys [conn]} @tu/test-server]
    (testing "creation"
      (let [tasks                  [{:task/description "oi"}
                                    {:task/description "ola"}
                                    {:task/description "aio"}]
            {db-created :db-after} (->> tasks
                                      tx/create-tasks
                                      (ds/transact! conn))
            created                [{:db/id            1
                                     :task/description "oi"
                                     :task/completed   false}
                                    {:db/id            2
                                     :task/description "ola"
                                     :task/completed   false}
                                    {:db/id            3
                                     :task/description "aio"
                                     :task/completed   false}]]
        (is (match? created
                    (q/get-tasks-by-ids-db db-created [{:db/id 1}
                                                       {:db/id 2}
                                                       {:db/id 3}])))
        (is (match? created  (q/get-all-tasks-db db-created)))))

    (testing "completion"
      (let [completion               [{:db/id 2}
                                      {:db/id 3}]
            {db-completed :db-after} (->> completion
                                        tx/complete-tasks
                                        (ds/transact! conn))
            completed                [{:db/id            1,
                                       :task/description "oi",
                                       :task/completed   false}
                                      {:db/id            2,
                                       :task/description "ola",
                                       :task/completed   true}
                                      {:db/id            3,
                                       :task/description "aio",
                                       :task/completed   true}]]
        (is (match? completed (q/get-all-tasks-db db-completed)))))

    (testing "uncompletion"
      (let [uncompletion               [{:db/id 1}
                                        {:db/id 3}]
            {db-uncompleted :db-after} (->> uncompletion
                                          tx/uncomplete-tasks
                                          (ds/transact! conn))
            uncompleted                [{:db/id            1,
                                         :task/description "oi",
                                         :task/completed   false}
                                        {:db/id            2,
                                         :task/description "ola",
                                         :task/completed   true}
                                        {:db/id            3,
                                         :task/description "aio",
                                         :task/completed   false}]]
        (is (match? uncompleted (q/get-all-tasks-db db-uncompleted)))))

    (testing "updating"
      (let [updating-data          [{:db/id 2 :task/description "alo"}
                                    {:db/id 3 :task/description "ioa"}]
            actual-data            (q/get-tasks-by-ids-db
                                    (ds/db conn)
                                    updating-data)
            {db-updated :db-after} (->> updating-data
                                      (tx/update-tasks actual-data)
                                      (ds/transact! conn))
            updated-data           [{:db/id            1,
                                     :task/description "oi",
                                     :task/completed   false}
                                    {:db/id            2,
                                     :task/description "alo",
                                     :task/completed   true}
                                    {:db/id            3,
                                     :task/description "ioa",
                                     :task/completed   false}]]
        (is (match? updated-data (q/get-all-tasks-db db-updated)))))

    (testing "deletion"
      (let [deletion-data          [{:db/id 1}
                                    {:db/id 3}]
            actual-data            (q/get-tasks-by-ids-db
                                    (ds/db conn)
                                    deletion-data)
            {db-deleted :db-after} (->> deletion-data
                                      (tx/delete-tasks actual-data)
                                      (ds/transact! conn))
            after-deletion         [{:db/id 2, :task/description "alo", :task/completed true}]]
        (is (match? after-deletion (q/get-all-tasks-db db-deleted)))))))
