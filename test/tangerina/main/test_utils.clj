(ns tangerina.main.test-utils
  (:require [tangerina.main.core :as core]
            [tangerina.main.datascript.core :as db]))

(def test-server (atom nil))

(defn setup-teardown-db [system f]
  (let [server (core/prep-server test-server system)]
    (swap! server db/start-db!)
    (f)
    (swap! server db/stop-db!)))
