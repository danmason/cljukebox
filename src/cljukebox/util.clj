(ns cljukebox.util
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [medley.core :as medley])
  (:import [java.util.function Consumer Function]
           discord4j.core.object.entity.channel.MessageChannel
           discord4j.core.event.domain.message.MessageCreateEvent
           discord4j.core.spec.EmbedCreateSpec))

(defn read-config []
  (let [config-file (io/file "config.edn")]
    (if (.exists config-file)
      (edn/read-string (slurp config-file))
      {:token nil
       :default-prefix "^"})))

;; Bot state
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
  (let [message (.getMessage message-event)]
    {:guild-id (-> message-event .getGuildId .get .asString)
     :message-channel (.getChannel message )
     :content (.getContent message)}))

(defn merge-to-config [m]
  (let [current-config @!config
        updated-config (medley/deep-merge current-config m)]
    (reset! !config updated-config)
    (spit "config.edn" updated-config)))

(defn get-prefix [guild-id]
  (let [{:keys [default-prefix] :as config-map} @!config]
    (or (get-in config-map [guild-id :prefix]) default-prefix)))

(defn send-message
  ([message-channel content]
   (.flatMap message-channel (as-function (fn [channel] (.createMessage channel content))))))

(defn send-embed
  ([message-channel {:keys [title description fields]}]
   (.flatMap message-channel
             (as-function (fn [channel]
                            (.createEmbed channel
                                          (as-consumer (fn [embed-spec]
                                                         (reduce
                                                          (fn [embed {:keys [name value inline] :as field}]
                                                            (.addField embed name value (some? inline)))
                                                          (-> embed-spec
                                                              (.setTitle title)
                                                              (.setDescription description))
                                                          fields)))))))))
