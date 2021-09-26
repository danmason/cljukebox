(ns cljukebox.player
  (:require [cljukebox.util :as util])
  (:import [cljukebox AudioTrackScheduler LavaPlayerAudioProvider]
           [com.sedmelluq.discord.lavaplayer.player AudioLoadResultHandler DefaultAudioPlayerManager]
           com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
           [com.sedmelluq.discord.lavaplayer.track.playback AudioFrameBufferFactory NonAllocatingAudioFrameBuffer]
           java.time.Duration))

;; Player State
(def !guild-audio-managers (atom {}))
(def !voice-connections (atom {}))

(def ^AudioFrameBufferFactory non-allocating-frame-buffer-factory
  (reify com.sedmelluq.discord.lavaplayer.track.playback.AudioFrameBufferFactory
    (create [this buffer-duration audio-format stopping]
      (NonAllocatingAudioFrameBuffer. buffer-duration audio-format stopping))))

(def player-manager
  (let [player-manager (DefaultAudioPlayerManager.)]
    (-> player-manager
        (.getConfiguration)
        (.setFrameBufferFactory non-allocating-frame-buffer-factory))
    (AudioSourceManagers/registerRemoteSources player-manager)
    player-manager))

(defn track->map [track]
  (let [track-info (.getInfo track)]
    {:title (.title track-info)
     :author (.author track-info)
     :length (-> track-info .length util/millis->time-str)
     :identifier (.identifier track-info)
     :isStream (.isStream track-info)
     :uri (.uri track-info)}))

(defn playlist->map [playlist]
  {:name (.getName playlist)
   :tracks (.getTracks playlist)
   :selected-track (.getSelectedTrack playlist)
   :is-search-result? (.isSearchResult playlist)})

(defn mk-player []
  (.createPlayer player-manager))

(defn mk-provider [player]
  (LavaPlayerAudioProvider. player))

(defn mk-scheduler [player]
  (AudioTrackScheduler. player))

(defn mk-guild-audio-manager [guild-id]
  (let [player (mk-player)
        scheduler (mk-scheduler player)
        _ (.addListener player scheduler)
        guild-audio-manager {:guild-id guild-id
                             :player player
                             :provider (mk-provider player)
                             :scheduler scheduler}]
    (swap! !guild-audio-managers assoc guild-id guild-audio-manager)
    guild-audio-manager))

(defn get-guild-audio-manager [guild-id]
  (if-let [current-guild-audio-manager (get @!guild-audio-managers guild-id)]
    current-guild-audio-manager
    (mk-guild-audio-manager guild-id)))

(defn get-current-connection [guild-id]
  (get @!voice-connections guild-id))

(defn connect-to-voice [{:keys [guild-id provider] :as guild-manager} voice-channel]
  (when-not (get @!voice-connections guild-id)
    (let [voice-connection (-> (.join voice-channel (util/as-consumer (fn [spec] (.setProvider spec provider))))
                               .block)]
      (swap! !voice-connections assoc guild-id voice-connection))))

(defrecord AudioLoadedHandler [scheduler message-channel]
  AudioLoadResultHandler
  (trackLoaded [_ track]
    (let [{:keys [title]} (track->map track)]
      (.play scheduler track)
      (util/send-message message-channel (format "`%s` has been added to the queue" title))))
  (playlistLoaded [_ playlist]
    (let [{:keys [name tracks]} (playlist->map playlist)]
      (run! #(.play scheduler %) tracks)
      (util/send-message message-channel (format "`%s` has been added to the queue" name)))
    (util/send-message message-channel (format "Playlist `%s` has been added to the queue" (.getName playlist))))
  (noMatches [_]
    (util/send-message message-channel "No song found at given url"))
  (loadFailed [_ exception]
    (util/send-message message-channel "Track failed to load")))

(defn mk-audio-handler [scheduler message-channel] (AudioLoadedHandler. scheduler message-channel))
