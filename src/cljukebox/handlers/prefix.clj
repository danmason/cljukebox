(ns cljukebox.handlers.prefix
  (:require [cljukebox.util :as util]
            [clojure.string :as string]))

(defn set-prefix [!config !state {:keys [channel-id guild-id content] :as data}]
  (let [split-data (string/split content #" ")]
    (if (= 2 (count split-data))
      (let [[_ new-prefix] split-data]
        (util/merge-to-config !config {guild-id {:prefix new-prefix}})
        (util/send-message !state channel-id (format "Command prefix set to `%s`" new-prefix)))
      (util/send-message !state channel-id (format "Command prefix is currently set to `%s`" (util/get-prefix @!config guild-id))))))

(def prefix-data
  {:doc "Sets the server wide command prefix (default is `^`)"
   :usage-str "prefix <new-prefix>"
   :handler-fn set-prefix })