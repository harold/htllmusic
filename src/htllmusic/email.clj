(ns htllmusic.email
  (:require [clojure.java.io :as io]
            [org.httpkit.client :as http]))

(def email-config* (atom nil))

(defn- populate-config
  []
  (->> "email-config.edn"
       (io/resource)
       (slurp)
       (clojure.edn/read-string)
       (reset! email-config*)))

(defn send-email
  [to subject body]
  (if-not @email-config* (populate-config))
  (let [domain (:domain @email-config*)
        email-key (:key @email-config*)]
    (http/post (str "https://api.mailgun.net/v3/" domain "/messages")
               {:basic-auth ["api" email-key]
                :form-params {:from "admin@htllmusic.com"
                              :to to
                              :subject subject
                              :html body}})))
