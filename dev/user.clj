(ns user
  (:require
   [tangerina.main.core :as backend]
   [shadow.cljs.devtools.api :as shadow.api]
   [shadow.cljs.devtools.server :as shadow.server]))

(defn -main
  {:shadow/requires-server true}
  [& _]
  (backend/-main)
  (shadow.server/start!)
  (shadow.api/watch :todo-mvc))

#_(-main)

#_(server/stop-server!)
