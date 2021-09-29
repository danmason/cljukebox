(ns cljukebox.handlers.prefix
  (:require [cljukebox.util :as util]))

(defn set-prefix
  ([{:keys [content message-channel guild-id] :as data}]
   (if-let [new-prefix (util/get-arguments content)]
     (set-prefix data {:new-prefix new-prefix})
     (util/send-message message-channel (format "Command prefix is currently set to `%s`" (util/get-prefix guild-id)))))
  ([{:keys [message-channel guild-id] :as data} {:keys [new-prefix] :as opts}]
   (util/merge-to-config {guild-id {:prefix new-prefix}})
   (util/send-message message-channel (format "Command prefix set to `%s`" new-prefix))))

(def handler-data
  {:doc "Sets the server wide command prefix (default is `^`)"
   :usage-str "prefix <new-prefix>"
   :handler-fn set-prefix})
