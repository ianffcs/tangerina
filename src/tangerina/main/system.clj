(ns tangerina.main.system
  (:require [datascript.core :as ds]
            [com.walmartlabs.lacinia.schema :as lacinia.schema]
            [com.walmartlabs.lacinia.util :as lacinia.util]
            [com.walmartlabs.lacinia.pedestal2 :as lp]
            [io.pedestal.http :as http]
            [io.pedestal.test :refer [response-for]]
            [tangerina.main.datascript :as tg-ds]
            [tangerina.main.atom-db :refer [atom-impl]]
            [tangerina.main.lacinia :refer [lacinia-impl]]))

;; TODO use edn?

(defn create-system
  [{::keys [] :as env}]
  (let [conn                (ds/create-conn tg-ds/schema)
        state               (atom {})
        lacinia-schema      {:objects   {:Task {:fields {:id          {:type 'Int}
                                                         :checked     {:type 'Boolean}
                                                         :description {:type 'String}}}}
                             :queries   {:impl  {:type    'String
                                                 :resolve :query/impl}
                                         :tasks {:type    '(list Task)
                                                 :resolve :query/tasks}}
                             :mutations {:create_task {:type    'Task
                                                       :args    {:description {:type 'String}}
                                                       :resolve :mutation/create-task}}}
        ds-gql-schema       (-> lacinia-schema
                               (lacinia.util/attach-resolvers (tg-ds/datascript-impl {::conn conn}))
                               lacinia.schema/compile)
        atom-http-schema    (-> lacinia-schema
                               (lacinia.util/attach-resolvers (atom-impl {::state state}))
                               lacinia.schema/compile)
        lacinia-wtf-schema  (-> lacinia-schema
                               (lacinia.util/attach-resolvers (lacinia-impl {::lacinias [ds-gql-schema
                                                                                         atom-http-schema]}))
                               lacinia.schema/compile)
        atom-http-service   (-> atom-http-schema
                               (lp/default-service {})
                               (assoc ::http/port 8888))
        lacinia-wtf-service (-> lacinia-wtf-schema
                               (lp/default-service {})
                               (assoc ::http/port 8890))
        ds-http-service     (-> ds-gql-schema
                               (lp/default-service {})
                               (assoc ::http/port 8889))]
    (assoc env ::conn conn
           ::state state
           ::http-services [::ds-http-service
                            ::lacinia-wtf-service
                            ::atom-http-service]
           ::ds-http-service ds-http-service
           ::lacinia-wtf-service lacinia-wtf-service
           ::atom-http-service atom-http-service)))
