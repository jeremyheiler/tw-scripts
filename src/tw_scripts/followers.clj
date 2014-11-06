(ns tw-scripts.followers
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
             :url "https://api.twitter.com/1.1/followers/list.json"
             :throw-exceptions? false
             :query-params {:screen_name (:owner conf)
                            :count (min (:followers-per-request conf 20) 200)
                            :skip_status true}
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

(comment
  (defn get-followers
    ([] (get-followers (mk-req)))
    ([req]
       (lazy-seq
        (let [res (http/request req)]
          (if (= 200 (:status res))
            (let [body (json/decode (:body res))
                  users (map #(select-keys % ["id_str" "screen_name"]) (get body "users"))
                  buf (ArrayChunk. (into-array users) 0 (count users))
                  next-cursor (get body "next_cursor")]
              (if (= 0 next-cursor)
                (chunk-cons buf nil)
                (chunk-cons buf (get-followers (mk-req next-cursor)))))
            (throw (ex-info "http error" res))))))))

(defn get-followers-from-cursor
  [conf cursor]
  (lazy-seq
   (let [res (http/request (mk-req conf cursor))]
     (if (= 200 (:status res))
       (let [body (json/decode (:body res))
             users (map #(select-keys % ["id_str" "screen_name"]) (get body "users"))
             buf (ArrayChunk. (into-array users) 0 (count users))
             next-cursor (get body "next_cursor")]
         (if (= 0 next-cursor)
           (chunk-cons buf nil)
           (chunk-cons buf (get-followers-from-cursor conf next-cursor))))
       (throw (ex-info "http error" res))))))

(defn get-followers
  [conf]
  (get-followers-from-cursor conf nil))

(defn print-followers
  [rows]
  (when (seq rows)
    (letfn [(width-fn [k]
              (apply max (count (str k)) (map #(count (str (get % k))) rows)))]
      (doseq [row rows]
        (println (format (str "%"
                              (width-fn "id_str")
                              "s %-"
                              (width-fn "screen_name")
                              "s")
                         (get row "id_str")
                         (get row "screen_name")))))))

(defn save
  ([conf users]
     (save conf users (.format (SimpleDateFormat. "yyy-MM-dd") (Date.))))
  ([conf users date-str]
     ;; TODO: also grab the "id" field so no parsing is needed.
     ;; TODO: allow for a custom file name format.
     (let [sorted-users (sort-by #(Long/parseLong (get % "id_str")) users)]
       (spit (str (:data-dir conf (System/getProperty "user.home")) "/followers-" date-str)
             (with-out-str (print-followers sorted-users))))))

(defn -main
  [& args]
  (try
    (let [conf (util/load-conf)
          followers (get-followers conf)]
      ;; TODO: say where the file is saved?
      (println "Downloaded" (count followers) "followers.")
      (save conf followers))
    (catch ExceptionInfo e
      (pprint (ex-data e)))
    (catch Exception e
      (repl/pst e))))
