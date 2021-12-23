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

(defn audio-queue
  ([data]
   (audio-queue data nil))
  ([{:keys [message-channel guild-id] :as data} _opts]
   (let [{:keys [scheduler] :as guild-manager} (player/get-guild-audio-manager guild-id)]
     (if-let [current-track (.nowPlaying scheduler)]
       (util/send-embed message-channel (mk-embed current-track))
       (util/send-message message-channel "No song is currently playing on the bot.")))))

(def handler-data
  {:doc "Outputs the currently playing song in the queue"
   :usage-str "nowplaying"
   :handler-fn audio-queue})
