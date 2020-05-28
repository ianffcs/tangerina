(ns tangerina.client.request-graphql
  (:require [cljs.core.async :as async]
            ["graphql-request" :as gqlr]
            [com.wsscode.pathom.connect.graphql2 :as pcgql]))

(def graphql-app
  {:url "http://localhost:8888/graphql"})

(defn gql-request! [url eql]
  (let [chan    (async/promise-chan)
        raise   (fn request-async-raise [ex]
                  (async/put! chan (if (nil? ex)
                                     ::nil
                                     ex)))
        respond (fn request-async-respond [response]
                  (try
                    (async/put! chan response)
                    (catch #?(:clj  Throwable
                              :cljs :default) e
                      (raise e))))]
    (-> (gqlr/request url
                      (pcgql/query->graphql eql
                                            {}))
        (.then (fn [response]
                 (respond (js->clj response :keywordize-keys true))))
        (.catch (fn [err]
                  (raise err))))
    chan))

(defn execute! [eql]
  (async/go
    (->> (gql-request! (get graphql-app :url) eql)
         async/<!)))

#_#_(async/go (->> `[{(createTask {:description "foi!"})
                      [:id :checked :description]}]
                   execute!
                   async/<!
                   prn))

(async/go (->> [{:tasks
                 [:id :checked :description]}]
               execute!
               async/<!
               :tasks
               (sort-by :id)
               prn))
