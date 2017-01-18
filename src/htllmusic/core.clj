(ns htllmusic.core
  (:require [ring.util.response :refer [response redirect]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [org.httpkit.server :as server]
            [bidi.bidi :as bidi]
            [bidi.ring :as bidi-ring]
            [hiccup.page :refer [html5]]
            [garden.core :refer [css]]
            [garden.units :refer [px]]
            [htllmusic.resource :as resource])
  (:gen-class))

(defn htll-css
  []
  (css [[:* {:margin 0 :padding 0}]
        [:body {:padding (px 10)
                :font-family :sans-serif
                :color :#222
                :background :#f8f8f8}]]))

(defn page
  [content]
  (response (html5 [:head
                    [:title "htllmusic"]
                    [:style (htll-css)]]
                   [:body content])))

(defn index-handler
  [req]
  (page [:div.index-page
         [:h1 "HTLL"]
         [:div [:tt "I N D E X"]]
         [:div [:a {:href "/releases"} "Releases"]]
         [:div [:a {:href "/artist-login"} "Artist login"]]]))

(defn artist-login-handler
  [req]
  (condp = (:request-method req)
    :get
    (page [:div.artist-login
           [:h1 "HTLL"]
           [:div [:tt "A R T I S T - L O G I N"]]
           [:form {:method :post :action "/artist-login"}
            [:input {:type :email :name "email" :autofocus true :placeholder "Email Address"}]
            [:input {:type :submit}]]
           [:a {:href "/"} "Index"]])
    :post
    (let [email (:email (:params req))]
      (println email)
      (comment "Send email...")
      (page [:div.artist-login
             [:h1 "HTLL"]
             [:p "Check your email for login link..."]
             [:a {:href "/"} "Index"]]))))

(defn releases-handler
  [req]
  (let [releases (resource/retrieve {:resource/type :resource.type/release})]
    (page [:div.releases-page
           [:div [:tt "R E L E A S E S"]]
           [:div "Create release:"
            [:form {:action "/release/create" :method :post}
             [:table
              [:tr [:td "ID"] [:td [:input {:type :text :name "id"}]]]
              [:tr [:td "Name"] [:td [:input {:type :text :name "name"}]]]
              [:tr [:td "Date"] [:td [:input {:type :date :name "date"}]]]]
             [:input {:type :submit}]]]
           (into [:table]
                 (for [release releases]
                   [:tr
                    [:td (:release/id release)]
                    [:td (:release/name release)]
                    [:td (:release/date release)]]))
           [:div [:a {:href "/"} "Index"]]])))

(defn- parse-date
  [date]
  (-> (java.text.SimpleDateFormat. "yyyy-MM-dd")
      (.parse date)))

(defn release-create-handler
  [req]
  (let [{:keys [id name date]} (:params req)]
    (resource/store {:resource/type :resource.type/release
                     :release/id id
                     :release/name name
                     :release/date (parse-date date)}))
  (redirect "/releases"))

(def handler
  (bidi-ring/make-handler
   ["/" {"" #'index-handler
         "artist-login" #'artist-login-handler
         "releases" #'releases-handler
         "release/" {"create" #'release-create-handler}}]))

(defonce stop-fn* (atom nil))

(defn start
  [& {:keys [port]
      :or {port 8080}}]
  (let [app (-> #'handler
                wrap-keyword-params
                wrap-params
                (wrap-resource "public"))]
    (->> (server/run-server app {:port port})
         (reset! stop-fn*))
    (println "Started server on port" port))
  (resource/start))

(defn stop
  []
  (when-let [stop-fn @stop-fn*]
    (stop-fn)
    (reset! stop-fn* nil)
    (println "Stopped server..."))
  (resource/stop))

(defn restart
  []
  (stop)
  (start)
  (resource/restart))

(defn -main
  [& args]
  (start))
