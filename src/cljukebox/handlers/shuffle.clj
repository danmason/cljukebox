(ns cljukebox.handlers.shuffle
  (:require [cljukebox.player :as player]
            [cljukebox.util :as util]))

(defn shuffle-scheduler [{:keys [message-channel]} {:keys [scheduler]}]
  (.shuffle scheduler)
  (util/send-message message-channel ":twisted_rightwards_arrows: **Bot queue shuffled!**"))

(defn shuffle-playlist
  ([data]
   (shuffle-playlist data nil))
  ([{:keys [message-channel guild-id] :as data} _opts]
   (if-let [guild-manager (player/get-guild-audio-manager guild-id)]
     (shuffle-scheduler data guild-manager)
     (util/missing-audio-manager-message message-channel))))

(def handler-data
  {:doc "Shuffles the contents of the playlist"
   :long-doc "Shuffles the current playlist contents on the bot. Note - this will *not* retain the previous state of the playlist, and any new songs will be added to the end of the list as normal."
   :usage-str "shuffle"
   :handler-fn shuffle-playlist})
