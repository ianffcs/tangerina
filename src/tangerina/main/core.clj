(ns tangerina.main.core
  (:require
   [tangerina.main.system :as system]
   [tangerina.main.datascript.core :as db]
   [tangerina.main.http.pedestal :as http]))

(defonce server
  (atom nil))

(defn prep-server [server system]
  (reset! server system)
  server)

(defn start-server! [server]
  (swap! server db/start-db!)
  (swap! server http/start-http!))

(defn stop-server! [server]
  (swap! server http/stop-http!)
  (swap! server db/stop-db!))

(defn -main [env]
  (->> env
     system/system-map
     (prep-server server)
     start-server!))

#_ (->> ::system/dev
        system/system-map
        #_(prep-server server))
#_#_#_ (prn @server)
(start-server! server)
(stop-server! server)
