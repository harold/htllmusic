(ns htllmusic.core
  (:require [clojure.string :as s]
            [ring.util.response :refer [response redirect]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [prone.middleware :refer [wrap-exceptions]]
            [prone.debug :refer [debug]]
            [org.httpkit.server :as server]
            [bidi.bidi :as bidi]
            [bidi.ring :as bidi-ring]
            [hiccup.page :refer [html5]]
            [garden.core :refer [css]]
            [garden.units :refer [px]]
            [htllmusic.resource :as resource]
            [htllmusic.email :as email])
  (:gen-class))

(defn htll-css
  []
  (css {:pretty-print? false}
       [[:* {:margin 0 :padding 0}]
        [:body {:padding (px 10)
                :font-family :sans-serif
                :color :#222
                :background :#f8f8f8}]]))

(defn page
  [content & {:keys [:cookies]}]
  (merge
   (response (html5 [:head
                     [:title "htllmusic"]
                     [:style (htll-css)]]
                    [:body content]))
   (if cookies {:cookies cookies})))

(defn cookie
  [k v & {:keys [days
                 path]
          :or {days 1000
               path "/"}}]
  {k {:value v
      :max-age (* 60 60 24 days)
      :path path}})

(defn index-handler
  [req]
  (page [:div.index-page
         [:h1 "HTLL"]
         [:div [:tt "I N D E X"]]
         [:div [:a {:href "/releases"} "Releases"]]
         (if (:user req)
           [:div [:a {:href "/logout"} "Logout"]]
           [:div [:a {:href "/artist-login"} "Artist login"]])]))

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
    (let [email (-> req :params :email s/trim s/lower-case)
          user (resource/find-by-value :resource.type/user :user/email email)]
      (when user
        (let [login-key (java.util.UUID/randomUUID)
              id (:resource/id user)
              existing-keys (:user/login-keys user #{})
              link (format "http://localhost:8080/l/%s" login-key)
              body (format "Click this link to login:\n%s" link)]
          (resource/store {:resource/type :resource.type/user
                           :resource/id id
                           :user/login-keys (conj existing-keys login-key)})
          (email/send-email email "[htllmusic] Login Link" body)))
      (page [:div.artist-login
             [:h1 "HTLL"]
             [:p "Check your email for login link..."]
             [:a {:href "/"} "Index"]]))))

(defn login-key-str->user
  [k]
  (let [login-key (java.util.UUID/fromString k)]
    (->> (resource/retrieve {:resource/type :resource.type/user})
         (filter (fn [{:keys [:user/login-keys]}]
                   (login-keys login-key)))
         (first))))

(defn l-handler
  [req]
  (let [k (-> req :params :id)
        user (login-key-str->user k)]
    (if user
      (page [:div.l-page
             [:p (format "Hello, %s." (:user/name user))]
             [:a {:href "/"} "Index"]]
            :cookies (cookie "k" k))
      (redirect "/"))))

(defn logout-handler
  [req]
  (-> (redirect "/")
      (assoc :cookies (cookie "k" "" :days 0))))

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

(defn users-handler
  [req]
  (let [users (resource/retrieve {:resource/type :resource.type/user})]
    (page [:div.users-page
           [:div [:tt "U S E R S"]]
           (into [:ol]
                 (for [user (sort-by :user/name users)]
                   [:li (:user/name user) " (" (:user/email user) ")"]))
           [:a {:href "/"} "Index"]])))

(defn wrap-auth
  [handler]
  (fn [req]
    (if-let [k (get-in req [:cookies "k" :value])]
      (if-let [user (login-key-str->user k)]
        (handler (assoc req :user user))
        (handler req))
      (handler req))))

(def handler
  (bidi-ring/make-handler
   ["/" {"" #'index-handler
         "artist-login" #'artist-login-handler
         "l/" {[:id] #'l-handler}
         "logout" #'logout-handler
         "releases" #'releases-handler
         "release/" {"create" #'release-create-handler}
         "users" #'users-handler}]))

(defonce stop-fn* (atom nil))

(defn start
  [& {:keys [port]
      :or {port 8080}}]
  (let [app (-> #'handler
                wrap-exceptions ;; TODO: not in production
                wrap-auth
                wrap-cookies
                wrap-keyword-params
                wrap-params
                (wrap-resource "public"))]
    (->> (server/run-server app {:port port})
         (reset! stop-fn*))
    (println "Started server on port" port))
  (resource/start)
  :started)

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
  (resource/restart)
  :restarted)

(defn -main
  [& args]
  (start))
