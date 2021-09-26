(ns cljukebox.core
  (:require [clojure.string :as string]
            [cljukebox.util :as util]
            [cljukebox.handlers :as handlers])
  (:import [discord4j.core DiscordClient GatewayDiscordClient]
           [discord4j.core.object.presence Presence Status Activity]
           discord4j.core.event.domain.lifecycle.ReadyEvent
           discord4j.core.event.domain.message.MessageCreateEvent
           reactor.core.publisher.Mono))

(defn on-message [message-event]
  (let [{:keys [guild-id message-channel content] :as data} (util/message-event->map message-event)
        prefix (util/get-prefix guild-id)
        handler-fn (handlers/get-handler-fn content prefix)]
    (if handler-fn
      (handler-fn data)
      (Mono/empty))))

(defn on-bot-ready [^ReadyEvent ready-event]
  (-> ready-event
      (.getSelf)
      (.getClient)
      (.updatePresence (Presence/online (Activity/playing "Type '^help' for a list of commands")))
      (.block))
  (Mono/empty))

(defn handle-client [^GatewayDiscordClient client]
  (let [on-login (-> client
                     (.on ReadyEvent (util/as-function on-bot-ready))
                     (.then))
        on-message (-> client
                       (.on MessageCreateEvent (util/as-function on-message))
                       (.then))]
    (.and on-login on-message)))

(defn start-bot! [token]
  (-> (DiscordClient/create token)
      (.withGateway (util/as-function handle-client))
      (.block)))

(defn -main [& [api-token]]
  (when api-token
    (util/merge-to-config {:token api-token}))
  (if-let [token (util/get-api-token)]
    (start-bot! token)
    (println "API token not configured - pass as an argument when starting the bot.")))
