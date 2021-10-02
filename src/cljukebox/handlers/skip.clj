(ns cljukebox.handlers.skip
  (:require [cljukebox.player :as player]
            [cljukebox.util :as util]))

(defn skip-audio
  ([data]
   (skip-audio data nil))
  ([{:keys [message-channel guild-id] :as data} _opts]
   (let [{:keys [player scheduler] :as guild-manager} (player/get-guild-audio-manager guild-id)
         current-song (.getPlayingTrack player)]
     (if current-song
       (let [{:keys [title]} (player/track->map current-song)]
         (.skip scheduler)
         (util/send-message message-channel (format "Track: `%s` has been skipped" title)))
       (util/send-message message-channel "No song is currently playing on the bot - nothing skipped")))))

(def handler-data
  {:doc "Skips the currently playing song, playing the next song in the queue (if any is present)"
   :usage-str "skip"
   :handler-fn skip-audio})
