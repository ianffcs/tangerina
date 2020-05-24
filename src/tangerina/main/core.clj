(ns tangerina.main.core
  (:require [io.pedestal.http :as http]
            [tangerina.main.system :as sys]))


(defn start-system
  "start with (-> (create-system {}) start-system)"
  [{::keys [http-services]
    :as    env}]
  (reduce
   (fn [acc k]
     (update acc k #(-> %
                       http/create-server
                       http/start)))
   env http-services))

(defn stop-system
  [{::keys [http-services]
    :as    env}]
  (reduce
   (fn [acc k]
     (update acc k #(some-> %
                            http/stop)))
   env http-services))

(defn -main []
  (-> {}
     (sys/create-system)
     start-system))
