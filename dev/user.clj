(ns user
  (:require
   [tangerina.main.system :as system]
   [juxt.clip.repl :refer [start stop reset set-init! system]]))

(comment (set-init! #(system/system-config :dev)))

#_(start)
