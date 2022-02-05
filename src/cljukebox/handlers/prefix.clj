(ns cljukebox.handlers.prefix
  (:require [cljukebox.util :as util]))

(defn set-prefix
  ([{:keys [content message-channel guild-id] :as data}]
   (let [new-prefix (util/get-arguments content)]
     (set-prefix data {:new-prefix new-prefix})))
  ([{:keys [message-channel guild-id] :as data} {:keys [new-prefix] :as opts}]
   (if new-prefix
     (do
       (util/merge-to-config {guild-id {:prefix new-prefix}})
       (util/send-message message-channel (format "Command prefix has been changed to `%s`" new-prefix)))
     (util/send-message message-channel (format "Command prefix currently set to `%s`" (util/get-prefix guild-id))))))

(def handler-data
  {:doc "Sets the server wide command prefix (default is `^`)"
   :usage-str "prefix <new-prefix>"
   :args [{:name "new-prefix"
           :doc "New prefix string for the bot to use for commands"
           :required? false}]
   :handler-fn set-prefix})
