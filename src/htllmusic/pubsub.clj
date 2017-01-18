(ns htllmusic.pubsub)

(defonce subscriptions* (atom {}))

(defn subscribe
  [k f]
  (let [uuid (java.util.UUID/randomUUID)]
    (swap! subscriptions* assoc-in [k uuid] f)
    #(swap! subscriptions* update-in [k] dissoc uuid)))

(defn publish
  [k & args]
  (doseq [[_ f] (get @subscriptions* k)]
    (apply f args)))
