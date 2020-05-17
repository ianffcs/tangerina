(ns user
  (:require
   [tangerina.main.core :as backend]
   [tangerina.main.system :as system]
   [shadow.cljs.devtools.api :as shadow.api]
   [shadow.cljs.devtools.server :as shadow.server]))

(defn -main
  {:shadow/requires-server true}
  [& _]
  (backend/-main ::system/dev)
  (shadow.server/start!)
  (shadow.api/watch :todo-mvc))

#_(-main)

#_(server/stop-server!)
