(ns tangerina.main.pathom.resolvers
  (:require
   [com.wsscode.pathom.core :as p]
   [com.wsscode.pathom.connect :as pc]
   #?(:clj [clojure.test :refer [deftest is]])
   #?(:clj [clojure.core.async :refer [>! >!! <!! <! go chan buffer close! thread alts! alts!! timeout]]
      :cljs [cljs.core.async :refer [<! go]])))

(def users-table
  {1 {:user/id 1 :user/name "Sally" :user/age 32}
   2 {:user/id 2 :user/name "Joe" :user/age 22}
   3 {:user/id 3 :user/name "Fred" :user/age 11}
   4 {:user/id 4 :user/name "Bobby" :user/age 55}})

(def list-table
  {:friends {:list/id     :friends
             :list/label  "Friends"
             :list/people [{:user/id 1} {:user/id 2}]}
   :enemies {:list/id     :enemies
             :list/label  "Enemies"
             :list/people [{:user/id 4} {:user/id 3}]}})

(pc/defresolver user-resolver [env {:keys [user/id] :as params}]
  ;; The minimum data we must already know in order to resolve the outputs
  {::pc/input  #{:user/id}
   ;; A query template for what this resolver outputs
   ::pc/output [:user/name :user/age]}
  ;; normally you'd pull the person from the db, and satisfy the listed
  ;; outputs. For demo, we just always return the same person details.
  (get users-table id))

;; define a list with our resolvers
(def my-resolvers [user-resolver
                   #_user-address-resolver
                   #_address-resolver])

(def parser
  (p/parser
   {::p/env     {::p/reader               [p/map-reader
                                           pc/reader
                                           pc/open-ident-reader
                                           p/env-placeholder-reader]
                 ::p/placeholder-prefixes #{">"}}
    ::p/mutate  pc/mutate
    ::p/plugins [(pc/connect-plugin {::pc/register my-resolvers})
                 p/error-handler-plugin
                 p/trace-plugin]}))
(def pparser
  (p/parallel-parser
   {::p/env     {::p/reader               [p/map-reader
                                           pc/parallel-reader
                                           pc/open-ident-reader
                                           p/env-placeholder-reader]
                 ::p/placeholder-prefixes #{">"}}
    ::p/mutate  pc/mutate-async
    ::p/plugins [(pc/connect-plugin {::pc/register my-resolvers})
                 p/error-handler-plugin
                 p/trace-plugin]}))

(comment
  #?(:clj
     (deftest user-resolver-test
       (is (= (parser {} [{[:user/id 1] [:user/name]}])
              (<!! (pparser {} [{[:user/id 1] [:user/name]}]))
              {[:user/id 1] #:user{:name "Sally"}}))))

  #?(:cljs (parser {} [{[:user/id 1] [:user/name]}]))

  #?(:cljs
     (go (prn (<! (pparser {} [{[:user/id 1] [:user/name]}]))))))

;; How to go from :person/id to that person's details
#_(pc/defresolver user-address-resolver [env {:keys [user/id] :as params}]
    ;; The minimum data we must already know in order to resolve the outputs
    {::pc/input  #{:user/id}
     ;; A query template for what this resolver outputs
     ::pc/output [:user/name {:user/address [:address/id]}]}
    ;; normally you'd pull the person from the db, and satisfy the listed
    ;; outputs. For demo, we just always return the same person details.
    (get users-table :user/id))

;; how to go from :address/id to address details.
#_(pc/defresolver address-resolver [env {:keys [address/id] :as params}]
    {::pc/input  #{:address/id}
     ::pc/output [:address/city :address/state]}
    {:address/city  "Salem"
     :address/state "MA"})



;; setup for a given connect system


;; A join on a lookup ref (Fulcro ident) supplies the starting state of :person/id 1.
;; env can have anything you want in it (e.g. a Datomic/SQL connection, network service endpoint, etc.)
;; the concurrency is handled though core.async, so you have to read the channel to get the output


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; #?(:clj (<!! (parser {} [{[:user/id 1] [:user/name {:user/address [:address/city]}]}]))                         ;;
;;    :cljs (go (<! (.log js/console (parser {} [{[:user/id 1] [:user/name {:user/address [:address/city]}]}]))))) ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


                                        ; => {[:person/id 1] {:person/name "Tom" :person/address {:address/city "Salem"}}}
