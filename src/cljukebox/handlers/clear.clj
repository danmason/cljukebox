(ns cljukebox.handlers.clear
  (:require [cljukebox.player :as player]
            [cljukebox.util :as util]))

(defn clear-bot [{:keys [message-channel guild-id] :as data}]
  (let [{:keys [scheduler] :as guild-manager} (player/get-guild-audio-manager guild-id)]
    (.clear scheduler)
    (util/send-message message-channel ":wastebasket: **Bot queue cleared!**")))

(def handler-data
  {:doc "Will skip the currently playing track and remove all songs from the queue."
   :usage-str "clear"
   :handler-fn clear-bot})
