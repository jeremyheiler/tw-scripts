(ns tw-scripts.friends
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [clojure.pprint :refer [pprint]]
            [clojure.repl :as repl]
            [tw-scripts.auth :as auth]
            [tw-scripts.util :as util])
  (:import (clojure.lang ArrayChunk ExceptionInfo)
           (java.util Date)
           (java.text SimpleDateFormat)))

(defn mk-req
  [conf cursor]
  (let [req {:method :get
             :url "https://api.twitter.com/1.1/friends/list.json"
             :throw-exceptions? false
             :query-params {:screen_name (:owner conf)
                            :count (min (:friends-per-request conf 20) 200)
                            ;;:skip_status true
                            }
             :oauth-secrets {:consumer-secret (:consumer-secret conf)
                             :token-secret (:access-token-secret conf)}
             :oauth {:consumer-key (:consumer-key conf)
                     :nonce (auth/mk-nonce)
                     :signature-method "HMAC-SHA1"
                     :timestamp (str (int (/ (.getTime (Date.)) 1000)))
                     :token (:access-token conf)
                     :version "1.0"}}
        req (if cursor
              (assoc-in req [:query-params :cursor] cursor)
              req)
        req (assoc-in req [:headers "Authorization"] (auth/mk-authorization req))]
    req))

(defn get-friends-from-cursor
  [conf cursor]
  (lazy-seq
   (let [res (http/request (mk-req conf cursor))]
     (if (= 200 (:status res))
       (let [body (json/decode (:body res))
             users (get body "users")
             buf (ArrayChunk. (into-array users) 0 (count users))
             next-cursor (get body "next_cursor")]
         (if (= 0 next-cursor)
           (chunk-cons buf nil)
           (chunk-cons buf (get-friends-from-cursor conf next-cursor))))
       (throw (ex-info "http error" res))))))

(defn get-friends
  [conf]
  (get-friends-from-cursor conf nil))

(def f (SimpleDateFormat. "EEE MMM dd HH:mm:ss Z yyyy"))
