(ns tangerina.main.system
  (:require [com.walmartlabs.lacinia.pedestal :as lacinia-pedestal]
            [tangerina.main.datascript.schema :as ds-schema]
            [tangerina.main.datascript.core :as db]
            [io.pedestal.http :as http]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

;; TODO use edn?
(defn system-config
  [profile]
  ;; TODO: This could use edn/read and produce nicer error messages in case
  ;; of failure.
  (edn/read-string
   (slurp
    (io/resource
     (str "tangerina/main/system_" (name profile) ".edn")))))



(defn system-map [env]
  (cond (= env ::dev)  {::lacinia-pedestal/graphiql    true
                        ::lacinia-pedestal/ide-path    "/graphiql/"
                        ::lacinia-pedestal/get-enabled true
                        ::env                          ::dev
                        ::http/port                    8888
                        ::ds-schema/schema             (ds-schema/schema)}
        (= env ::prod) {::lacinia-pedestal/graphiql false
                        ::env                       ::prod
                        ::http/port                 3000
                        ::ds-schema/schema          (ds-schema/schema)}))
