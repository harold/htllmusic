(defproject htllmusic "0.1.0-SNAPSHOT"
  :description "netlabel shared commitment device"
  :url "http://www.htllmusic.com"
  :license {:name "Copyright 2017"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [http-kit "2.2.0"]
                 [bidi "2.0.16"]
                 [hiccup "1.0.5"]
                 [garden "1.3.2"]]
  :main ^:skip-aot htllmusic.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
