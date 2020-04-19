(ns tangerina.main.core-test
  (:require [tangerina.main.core :as core]
            [clj-http.client :as client]
            [com.wsscode.pathom.connect.graphql2 :as pg]
            [cheshire.core :as json]
            [clojure.test :refer [testing is deftest use-fixtures]]))



(defn http-fixture [f]
  (core/start-server! (core/system-map :dev))
  (f)
  (core/stop-server!))

(use-fixtures :once http-fixture)

(deftest web-server
  (testing "rest route"
    (is (= (select-keys (client/get "http://localhost:8888/greet")
                        [:body :status])
           {:body   "Hello, world!"
            :status 200})))
  (testing "graphql route"
    (is (= (select-keys (client/post "http://localhost:8888/graphql"
                                     {:content-type :graphql
                                      :body         (pg/query->graphql
                                                     `{:hello []} {})})
                        [:body :status])
           {:body   "{\"data\":{\"hello\":\"Hello, Clojurians!\"}}"
            :status 200}))))

(deftest graphql-crud-test
  (testing "create task"
    (is (= (select-keys
            (client/post "http://localhost:8888/graphql"
                         {:content-type :graphql
                          :body         (pg/query->graphql
                                         `[{(defineTask
                                              {:description "Foo"})
                                            [:description]}]
                                         {})
                          })
            [:body :status])
           {:body   "{\"data\":{\"defineTask\":{\"description\":\"Foo\"}}}"
            :status 200})))
  (testing "list tasks"
    (is (= (select-keys
            (client/post "http://localhost:8888/graphql"
                         {:content-type :graphql
                          :body         (pg/query->graphql
                                         `[{(:listTasks {:id 1})
                                            [:description]}]
                                         {})
                          })
            [:body :status])
           {:body   "{\"data\":{\"listTasks\":[{\"description\":\"Foo\"}]}}"
            :status 200}))))

(deftest issue-01
  ;; curl 'http://localhost:8888/graphql?query=mutation+%7B%0A++defineTask%28id%3A+%221%22%2C+description%3A+%22aaaaaaa%22%2C+completed%3A+true%2C+delete%3A+null%29+%7B%0A++++id%0A++++description%0A++++completed%0A++++delete%0A++%7D%0A%7D%0A' -H 'User-Agent: Mozilla/5.0 (X11; Linux x86_64; rv:75.0) Gecko/20100101 Firefox/75.0' -H 'Accept: */*' -H 'Accept-Language: pt-BR,pt;q=0.8,en-US;q=0.5,en;q=0.3' --compressed -H 'Referer: http://localhost:8888/index' -H 'Origin: http://localhost:8888' -H 'Connection: keep-alive' -H 'Cookie: org.cups.sid=e341d10ec7653ca1a95db7a3f6b95179; secret=8f50ddff-f114-4088-9b1e-46091019b638' --data ''
  (is false)
  )
