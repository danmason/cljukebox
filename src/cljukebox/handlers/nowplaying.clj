(ns cljukebox.handlers.nowplaying
  (:require [cljukebox.player :as player]
            [cljukebox.util :as util]))

(defn mk-embed [current-track]
  (let [{:keys [title author length uri identifier] :as track-info} (player/track->map current-track)]
    {:title title
     :url uri
     :fields [{:name "Author"
               :value author
               :inline true}
              {:name "Length"
               :value length
               :inline true}
              {:name "Audio ID"
               :value identifier
               :inline true}]}))

(defn output-now-playing [{:keys [message-channel]} {:keys [scheduler]}]
  (if-let [current-track (.nowPlaying scheduler)]
    (util/send-embed message-channel (mk-embed current-track))
    (util/send-message message-channel "No song is currently playing on the bot.")))

(defn now-playing
  ([data]
   (now-playing data nil))
  ([{:keys [message-channel guild-id] :as data} _opts]
   (if-let [guild-manager (player/get-guild-audio-manager guild-id)]
     (output-now-playing data guild-manager)
     (util/missing-audio-manager-message message-channel))))

(def handler-data
  {:doc "Outputs the currently playing song in the queue"
   :usage-str "nowplaying"
   :handler-fn now-playing})
