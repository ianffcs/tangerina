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

(use-fixtures :once (partial tu/setup-teardown-db test-system))

;; TODO transactions-test
(deftest crud-test
  (let [{::db/keys [conn]}             @tu/test-server
        ;;conn                         (doto (ds/create-conn))
        {:keys [db-after tempids]}         (ds/transact! conn
                                                     [;; manualmenete criando um cenário
                                                      {:db/id            -1
                                                       :task/description "abc"
                                                       :task/completed   false}])
        id-do-abc                      (ds/resolve-tempid db-after tempids -1)
        {db-complete-task :db-after}   (ds/transact! conn (tx/complete-task id-do-abc))
        {db-create-task :db-after}     (->> ["ola" "hello"]
                                          (mapcat tx/create-task)
                                          (ds/transact! conn))
        {db-update-task :db-after}     (->> [[2 "alo"] [1 "ioa"]]
                                          (mapcat (fn [[id desc]]
                                                    (tx/update-task id desc)))
                                          (ds/transact! conn))
        {db-uncomplete-task :db-after} (->> [3 1]
                                          (mapcat tx/uncomplete-task)
                                          (ds/transact! conn))
        {db-delete-task :db-after}     (->> [3 1]
                                          (mapcat tx/delete-task)
                                          (ds/transact! conn))]
    (testing
        "found task abc with completed true"
      (is (match? [{:db/id            1
                    :task/completed   true
                    :task/description "abc"}]
                  (q/tasks db-complete-task))))
    (testing
        "A task ola and hello created and appearing in tasks?"
      (is (match? [{:db/id            3
                    :task/completed   false
                    :task/description "hello"}
                   {:db/id            2
                    :task/completed   false
                    :task/description "ola"}
                   {:db/id            1
                    :task/completed   true
                    :task/description "abc"}]
                  (q/tasks db-create-task))))

    (testing "db updating description of task 2 to alo and 1 to ioa"
      (is (match? [{:db/id 3, :task/completed false, :task/description "hello"}
                   {:db/id 2, :task/completed false, :task/description "alo"}
                   {:db/id 1, :task/completed true, :task/description "ioa"}]
                  (q/tasks db-update-task))))

    (testing "db uncomplete tasks 1 and 3"
      (is (match? [{:db/id 3, :task/completed false, :task/description "hello"}
                   {:db/id 2, :task/completed false, :task/description "alo"}
                   {:db/id 1, :task/completed false, :task/description "ioa"}]
                  (q/tasks db-uncomplete-task))))

    (testing "delete tasks 1 and 2"
      (is (match? [{:db/id 2, :task/completed false, :task/description "alo"}]
                  (q/tasks db-delete-task))))))
