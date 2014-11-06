(ns tw-scripts.util
  (:require [clojure.edn :as edn]
            [clojure.string :as s])
  (:import (java.net URLEncoder)
           (java.util Base64)))

(defn map-keys-1
  "Transforms the top level keys in the given map to keywords, unless
  you provide f, then that function will be used as the transformer."
  [f m]
  (reduce-kv #(assoc %1 (f %2) %3) {} m))

(defn url-encode
  [s]
  (-> (URLEncoder/encode s "UTF-8")
      (s/replace #"\+" "%20")
      (s/replace #"%7E" "~")))

(defn base64-encode
  [byte-arr]
  (.encode (Base64/getEncoder) byte-arr))

(defn load-conf
  []
  (-> (str (System/getProperty "user.home") "/.tw.edn")
      (slurp)
      (edn/read-string)))
