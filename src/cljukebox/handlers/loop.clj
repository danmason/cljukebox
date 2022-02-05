(ns cljukebox.handlers.loop
  (:require [cljukebox.player :as player]
            [cljukebox.util :as util]))

(def !loop (atom false))

(defn loop-scheduler [{:keys [message-channel]} {:keys [scheduler]}]
  (let [should-loop (swap! !loop not)]
    (.setLoop scheduler should-loop)
    (if should-loop
      (util/send-message message-channel ":repeat: **Loop enabled!**")
      (util/send-message message-channel ":repeat: **Loop disabled!**"))))

(defn loop-audio
  ([data]
   (loop-audio data nil))
  ([{:keys [message-channel guild-id] :as data} _opts]
   (if-let [guild-manager (player/get-guild-audio-manager guild-id)]
     (loop-scheduler data guild-manager)
     (util/missing-audio-manager-message message-channel))))

(def handler-data
  {:doc "Will loop all songs that play on the bot - these will play until skipped"
   :usage-str "loop"
   :handler-fn loop-audio})
