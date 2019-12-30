(ns user
  (:require
   [juxt.clip-example.system :as system]
   [juxt.clip.repl :refer [start stop reset set-init! system]]))

(comment (set-init! #(system/system-config :dev)))

#_(start)
