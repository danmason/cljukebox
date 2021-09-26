(ns cljukebox.handlers.queue
  (:require [cljukebox.player :as player]
            [cljukebox.util :as util]))

(defn create-track-field [idx track]
  (let [{:keys [title author length] :as track-info} (player/track->map track)]
    {:name (format "%d: %s" idx title)
     :value (format "**Author**: %s | **Length**: %s" author length)}))

(defn audio-queue [{:keys [message-channel guild-id] :as data}]
  (let [{:keys [scheduler] :as guild-manager} (player/get-guild-audio-manager guild-id)
        track-queue (.queue scheduler)]
    (util/send-embed message-channel {:title "Bot Queue"
                                      :description "Currently queued songs on the bot:"
                                      :fields (map-indexed create-track-field track-queue)})))

(def handler-data
  {:doc "Outputs the current player queue for the server"
   :usage-str "queue"
   :handler-fn audio-queue})
