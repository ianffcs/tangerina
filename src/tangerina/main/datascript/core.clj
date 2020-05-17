(ns tangerina.main.datascript.core
  (:require [tangerina.main.datascript.schema :as ds-schema]
            [datascript.core :as ds]))

(defn start-db! [system]
  (-> system
     (assoc ::conn
            (ds/create-conn (get system ::ds-schema/schema)))))

(defn stop-db! [system]
  (-> system
     (dissoc ::conn)))
