(ns tangerina.main.core
  (:require
   [tangerina.main.system :as system]
   [tangerina.main.datascript.core :as db]
   [tangerina.main.http.pedestal :as http]))

(defonce server
  (atom nil))

(defn prep-server [env]
  (reset! server (system/system-map env))
  server)

(defn start-server! [server]
  (swap! server db/start-db!)
  (swap! server http/start-http!))

(defn stop-server! [server]
  (swap! server http/stop-http!)
  (swap! server db/stop-db!))

(defn -main [env]
  (-> env
     prep-server
     start-server!))

#_#_#_server
(start-server! server)
(stop-server! server)
