(ns cljukebox.util
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [medley.core :as medley])
  (:import [java.util.function Consumer Function]
           discord4j.core.object.entity.channel.MessageChannel
           discord4j.core.event.domain.message.MessageCreateEvent
           discord4j.core.object.command.ApplicationCommandInteractionOption
           discord4j.core.event.domain.interaction.ChatInputInteractionEvent
           discord4j.core.spec.EmbedCreateSpec))

(defn read-config []
  (let [config-file (io/file "config.edn")]
    (if (.exists config-file)
      (edn/read-string (slurp config-file))
      {:token nil
       :default-prefix "^"})))

;; Bot config
(def !config (atom (read-config)))

(defn ^Function as-function [f]
  (reify java.util.function.Function
    (apply [this arg] (f arg))))

(defn ^Consumer as-consumer [f]
  (reify java.util.function.Consumer
    (accept [this arg]
      (f arg))))

(defn get-api-token []
  (:token @!config))

(defn message-event->map [^MessageCreateEvent message-event]
  (let [message (.getMessage message-event)
        member (some-> message-event .getMember (.orElse nil))]
    {:guild-id (some-> message-event .getGuildId (.orElse nil) .asString)
     :message-channel (.getChannel message)
     :content (.getContent message)
     :member member
     :voice-channel (some-> member .getVoiceState .block .getChannel .block)}))

(defn chat-input-event->map [^ChatInputInteractionEvent chat-input-event]
  (let [interaction (.getInteraction chat-input-event)
        member (some-> interaction .getMember (.orElse nil))]
    {:guild-id (some-> interaction .getGuildId (.orElse nil) .asString)
     :message-channel (.getChannel interaction)
     :member member
     :voice-channel (some-> member .getVoiceState .block .getChannel .block)
     :command (.getCommandName chat-input-event)
     :args (some->> (.getOptions chat-input-event)
                    (not-empty)
                    (map (fn [^ApplicationCommandInteractionOption option]
                           (let [k (keyword (.getName option))
                                 v (some-> (.getValue option)
                                           (.orElse nil)
                                           (.asString))]
                             [k v])))
                    (into {}))}))

(defn merge-to-config [m]
  (let [current-config @!config
        updated-config (medley/deep-merge current-config m)]
    (reset! !config updated-config)
    (spit "config.edn" updated-config)))

(defn get-prefix [guild-id]
  (let [{:keys [default-prefix] :as config-map} @!config]
    (or (get-in config-map [guild-id :prefix]) default-prefix)))

(defn send-message [message-channel content]
  (.block
   (.flatMap message-channel (as-function (fn [channel] (.createMessage channel content))))))

(defn map->embed [embed-spec {:keys [title author color image-url thumbnail-url timestamp-instant url description fields]}]
  (reduce
   (fn [embed {:keys [name value inline] :as field}]
     (.addField embed name value (some? inline)))
   (cond-> embed-spec
     color (.setColor color)
     description (.setDescription description)
     image-url (.setImage image-url)
     thumbnail-url (.setThumbnail thumbnail-url)
     timestamp-instant (.setTimestamp timestamp-instant)
     title (.setTitle title)
     url (.setUrl url))
   fields))

(defn send-embed [message-channel embed-map]
  (.block
   (.flatMap message-channel
             (as-function (fn [channel]
                            (.createEmbed channel
                                          (as-consumer (fn [embed-spec] (map->embed embed-spec embed-map)))))))))

(defn millis->time-str [millis]
  (let [minutes (int (/ (/ millis 1000) 60))
        seconds (int (mod (/ millis 1000) 60))]
    (format "%d:%d" minutes seconds)))

(defn get-arguments [content-with-command]
  (let [args (rest (string/split content-with-command #" "))]
    (cond-> args
      (< (count args) 2) first)))
