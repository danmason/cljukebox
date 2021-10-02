(ns cljukebox.handlers.leave
  (:require [cljukebox.player :as player]
            [cljukebox.util :as util]))

(defn leave-voice-channel
  ([data]
   (leave-voice-channel data nil))
  ([{:keys [message-channel guild-id] :as data} _opts]
   (when-let [connection (player/get-current-connection guild-id)]
     (util/send-message message-channel "Leaving voice channel.")
     (-> connection .disconnect .block)
     (swap! player/!voice-connections dissoc guild-id))))

(def handler-data
  {:doc "Leave the currently connected voice channel"
   :usage-str "queue"
   :handler-fn leave-voice-channel})
