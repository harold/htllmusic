(ns htllmusic.resource
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [htllmusic.pubsub :as pubsub]))

(defonce index* (atom {}))
(defonce file-path  "./index.edn")
(defonce unsub-fn* (atom nil))

(defn retrieve
  [{:keys [:resource/id :resource/type]}]
  (let [primary-index (-> @index* type :primary)]
    (if id
      (get primary-index id)
      (vals primary-index))))

(defn store
  [{:keys [:resource/id :resource/type] :as resource} & {:keys [replace-resource]}]
  (cond
    (nil? type)
    (throw (ex-info "Stored resource needs :resource/type" resource))

    (nil? id)
    (let [id (java.util.UUID/randomUUID)]
      (swap! index* assoc-in [type :primary id] (assoc resource :resource/id id))
      (pubsub/publish :index-update))

    replace-resource
    (do (swap! index* assoc-in [type :primary id] resource)
        (pubsub/publish :index-update))

    :else
    (do (swap! index* update-in [type :primary id] merge resource)
        (pubsub/publish :index-update))))

(defn find-by-value
  [resource-type k v]
  (->> (retrieve {:resource/type resource-type})
       (filter #(= v (get % k)))
       first))

(defn persist
  []
  (future (spit file-path (pr-str @index*))))

(defn start
  []
  (reset! unsub-fn* (pubsub/subscribe :index-update persist))
  (let [f (io/file file-path)]
    (if (.exists f)
      (reset! index* (edn/read-string (slurp f))))))

(defn stop
  []
  (when-let [unsub-fn @unsub-fn*]
    (unsub-fn)
    (reset! unsub-fn* nil)))

(defn restart
  []
  (stop)
  (start))
