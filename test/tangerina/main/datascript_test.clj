(ns tangerina.main.datascript-test
  (:require [clojure.test :refer [testing deftest is]]
            [matcher-combinators.test :refer [match?]]
            [tangerina.main.datascript :as tg-ds]
            [datascript.core :as ds]))

(deftest crud-test
  (let [conn                                (doto (ds/create-conn tg-ds/schema))
        {:keys [db-after tempids]}          (ds/transact! conn
                                                          [{:db/id            -1
                                                            :task/description "abc"
                                                            :task/checked     false}])
        id-abc                              (ds/resolve-tempid db-after tempids -1)
        {db-check-task :db-after}           (ds/transact! conn (tg-ds/check-task id-abc))
        {db-create-task  :db-after
         tempids-created :tempids}          (->> [["ola" -2] ["hello" -3]]
                                               (mapcat (fn [[desc tempid]]
                                                         (tg-ds/create-task desc tempid)))
                                               (ds/transact! conn))
        [id-ola id-hello]                   (->> [-2 -3]
                                               (map #(ds/resolve-tempid db-after tempids-created %)))
        {db-set-description-task :db-after} (->> [[id-ola "alo"] [id-abc "ioa"]]
                                               (mapcat (fn [[id desc]]
                                                         (tg-ds/set-description-task id desc)))
                                               (ds/transact! conn))
        {db-uncheck-task :db-after}         (->> [id-hello id-abc]
                                               (mapcat tg-ds/uncheck-task)
                                               (ds/transact! conn))
        {db-delete-task :db-after}          (->> [id-hello id-abc]
                                               (mapcat tg-ds/delete-task)
                                               (ds/transact! conn))]
    (testing
        "found task abc with completed true"
      (is (match? [{:db/id            id-abc
                    :task/checked     true
                    :task/description "abc"}]
                  (tg-ds/tasks db-check-task))))
    (testing
        "A task ola and hello created and appearing in tasks?"
      (is (match? [{:db/id            3
                    :task/checked     false
                    :task/description "hello"}
                   {:db/id            2
                    :task/checked     false
                    :task/description "ola"}
                   {:db/id            1
                    :task/checked     true
                    :task/description "abc"}]
                  (tg-ds/tasks db-create-task))))

    (testing "db updating description of task 2 to alo and 1 to ioa"
      (is (match? [{:db/id 3, :task/checked false, :task/description "hello"}
                   {:db/id 2, :task/checked false, :task/description "alo"}
                   {:db/id 1, :task/checked true, :task/description "ioa"}]
                  (tg-ds/tasks db-set-description-task))))

    (testing "db uncomplete tasks 1 and 3"
      (is (match? [{:db/id 3, :task/checked false, :task/description "hello"}
                   {:db/id 2, :task/checked false, :task/description "alo"}
                   {:db/id 1, :task/checked false, :task/description "ioa"}]
                  (tg-ds/tasks db-uncheck-task))))

    (testing "delete tasks 1 and 2"
      (is (match? [{:db/id 2, :task/checked false, :task/description "alo"}]
                  (tg-ds/tasks db-delete-task))))))
