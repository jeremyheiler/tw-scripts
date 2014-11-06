(ns tw-scripts.auth
  (:require [clojure.string :as s]
            [tw-scripts.util :as util])
  (:import (javax.crypto Mac)
           (javax.crypto.spec SecretKeySpec)))

(defn mk-nonce
  []
  (let [bytes (reduce (fn [vect b]
                        (if (or (<= 48 b 57) (<= 65 b 90) (<= 97 b 122))
                          (let [vect (conj vect b)]
                            (if (= 32 (count vect))
                              (reduced vect)
                              vect))
                          vect))
                      []
                      (repeatedly (partial rand-int 256)))]
    (String. (byte-array bytes))))

(defn sign ^bytes
  [^bytes data ^bytes key algorithm]
  (let [mac (Mac/getInstance algorithm)]
    (.init mac (SecretKeySpec. key algorithm))
    (.doFinal mac data)))

(defn mk-parameter-str
  [req]
  (let [oauth (util/map-keys-1 #(str "oauth_" (s/replace (name %) #"-" "_")) (:oauth req))
        qp (util/map-keys-1 #(s/replace (name %) #"-" "_") (:query-params req))
        m (merge oauth qp)
        sorted-keys (sort (keys m))
        f (fn [vect k]
            (conj vect (str (util/url-encode k)
                            "="
                            (let [v (get m k)]
                              (if (string? v)
                                (util/url-encode v)
                                (util/url-encode (str v)))))))
        url-encoded-pairs (reduce f [] sorted-keys)
        parameter-str (s/join "&" url-encoded-pairs)]
    parameter-str))

(defn mk-signature-base
  [req]
  (str (s/upper-case (name (:method req)))
       "&"
       (util/url-encode (:url req))
       "&"
       (util/url-encode (mk-parameter-str req))))

(defn mk-signing-key
  [req]
  (str (util/url-encode (get-in req [:oauth-secrets :consumer-secret]))
       "&"
       (util/url-encode (get-in req [:oauth-secrets :token-secret]))))

(defn mk-signature
  [req]
  (String. (util/base64-encode
            (sign (.getBytes (mk-signature-base req) "UTF-8")
                  (.getBytes (mk-signing-key req) "UTF-8")
                  "HmacSHA1"))))

(defn mk-authorization
  [req]
  (let [oauth (:oauth req)
        authz (str "OAuth "
                   "oauth_consumer_key=\""
                   (util/url-encode (:consumer-key oauth))
                   "\", "
                   "oauth_nonce=\""
                   (util/url-encode (:nonce oauth))
                   "\", "
                   "oauth_signature=\""
                   (util/url-encode (mk-signature req))
                   "\", "
                   "oauth_signature_method=\""
                   (util/url-encode (:signature-method oauth))
                   "\", "
                   "oauth_timestamp=\""
                   (util/url-encode (:timestamp oauth))
                   "\", "
                   "oauth_token=\""
                   (util/url-encode (:token oauth))
                   "\", "
                   "oauth_version=\""
                   (util/url-encode (:version oauth))
                   "\"")]
    authz))


