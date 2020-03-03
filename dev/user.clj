(ns user
  (:require [tangerina.main.core :as server]
            [shadow.cljs.devtools.api :as shadow.api]
            [shadow.cljs.devtools.server :as shadow.server]))

(defn -main
  {:shadow/requires-server true}
  [& _]
  (server/start-server! (server/system-map :dev))
  (shadow.server/start!)
  (shadow.api/watch :todo-mvc))

#_(-main)

#_(server/stop-server!)
