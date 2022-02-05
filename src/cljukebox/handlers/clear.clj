(ns cljukebox.handlers.clear
  (:require [cljukebox.player :as player]
            [cljukebox.util :as util]))

(defn clear-scheduler [{:keys [message-channel]} {:keys [scheduler]}]
  (.clear scheduler)
  (util/send-message message-channel ":wastebasket: **Bot queue cleared!**"))

(defn clear-bot
  ([data]
   (clear-bot data nil))
  ([{:keys [message-channel guild-id] :as data} _opts]
   (if-let [guild-manager (player/get-guild-audio-manager guild-id)]
     (clear-scheduler data guild-manager)
     (util/missing-audio-manager-message message-channel))))

(def handler-data
  {:doc "Will skip the currently playing track and remove all songs from the queue."
   :usage-str "clear"
   :handler-fn clear-bot})
