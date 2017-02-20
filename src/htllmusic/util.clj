(ns htllmusic.util
  (:require [ring.util.response :refer [response]]
            [hiccup.page :refer [html5]]
            [garden.core :refer [css]]
            [garden.units :refer [px]]))

(defn htll-css
  []
  (let [link-color :#0000ee]
    (css {:pretty-print? false}
         [[:* {:margin 0 :padding 0}]
          [:body {:padding (px 10)
                  :font-family :sans-serif
                  :color :#222
                  :background :#f8f8f8}]
          [:.nav
           [:li {:display :inline-block
                 :margin-right (px 20)}
            [:a {:color link-color
                 :font-weight :bold
                 :text-decoration :none}
             [:&:visited {:color link-color}]]]]])))

(defn page
  [content & {:keys [:cookies]}]
  (merge
   (response (html5 [:head
                     [:title "htllmusic"]
                     [:style (htll-css)]]
                    [:body content]))
   (if cookies {:cookies cookies})))

(defn cookie
  [k v & {:keys [days path]
          :or {days 1000
               path "/"}}]
  {k {:value v
      :max-age (* 60 60 24 days)
      :path path}})

(defn nav
  [links]
  (into
   [:ol.nav]
   (for [{:keys [url link]} links]
     [:li [:a {:href url} link]])))
