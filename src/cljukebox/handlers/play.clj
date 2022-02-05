(ns cljukebox.handlers.play
  (:require [cljukebox.player :as player]
            [cljukebox.util :as util]))

(defn play-audio
  ([{:keys [voice-channel message-channel content] :as data}]
   (if-let [url (util/get-arguments content)]
     (play-audio data {:url url})
     (util/send-message message-channel "No media URL has been provided.")))
  ([{:keys [voice-channel message-channel guild-id] :as data} {:keys [url] :as opts}]
   (if-not voice-channel
     (util/send-message message-channel "You're not in a voice channel - join one to queue songs on the bot.")
     (let [_ (player/connect-to-voice data)
           {:keys [scheduler] :as guild-manager} (player/get-guild-audio-manager guild-id)]
       (.loadItemOrdered player/player-manager
                         guild-manager
                         url
                         (player/mk-audio-handler scheduler message-channel))))))

(def handler-data
  {:doc "Add a track to the bot's playlist"
   :long-doc "Add a track to the bot's playlist - if not currently playing anything, will join the bot to the calling user's voice channel. For a list of supported sources/file formats, see here: https://github.com/sedmelluq/lavaplayer#supported-formats"
   :usage-str "play <media-url>"
   :args [{:name "url"
           :doc "URL of the track you want to play"
           :required? true}]
   :handler-fn play-audio})
