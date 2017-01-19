(ns htllmusic.email
  (:require [org.httpkit.client :as http]))

(defn send-email
  [to subject body]
  (http/post "https://api.mailgun.net/v3/<DOMAIN>/messages"
             {:basic-auth ["api" "<KEY>"]
              :form-params {:from "admin@htllmusic.com"
                            :to to
                            :subject subject
                            :html body}}))
