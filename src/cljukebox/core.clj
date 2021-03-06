(ns cljukebox.core
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [cljukebox.util :as util]
            [cljukebox.handlers :as handlers]
            [cljukebox.player :as player])
  (:import [discord4j.core DiscordClient GatewayDiscordClient]
           [discord4j.core.object.presence ClientPresence Status ClientActivity]
           [discord4j.discordjson.json ApplicationCommandOptionData ApplicationCommandRequest]
           [discord4j.core.object.command ApplicationCommandOption$Type ApplicationCommand]
           discord4j.discordjson.json.ApplicationCommandData
           discord4j.core.event.domain.lifecycle.ReadyEvent
           discord4j.core.event.domain.interaction.ChatInputInteractionEvent
           discord4j.core.event.domain.message.MessageCreateEvent
           discord4j.core.event.domain.VoiceStateUpdateEvent
           discord4j.rest.RestClient
           reactor.core.publisher.Mono)
  (:gen-class))

(def !gateway-client (atom nil))

(defn on-message [message-event]
  (let [{:keys [guild-id content] :as data} (util/message-event->map message-event)
        prefix (util/get-prefix guild-id)
        handler-fn (handlers/get-handler-fn content prefix)]
    (when handler-fn
      (handler-fn data))
    (Mono/empty)))

(defn on-chat-input [chat-input-event]
  (let [{:keys [command args] :as data} (util/chat-input-event->map chat-input-event)
        handler-fn (get-in handlers/handlers [command :handler-fn])]
    (.subscribe (.deferReply chat-input-event))
    (handler-fn data args)
    (.subscribe (.deleteReply chat-input-event))
    (Mono/empty)))

(defn on-bot-ready [^ReadyEvent ready-event]
  (-> ready-event
      (.getSelf)
      (.getClient)
      (.updatePresence (ClientPresence/online (ClientActivity/playing "Type '^help' for a list of commands")))
      (.block))
  (Mono/empty))

(defn on-voice-state-update [^VoiceStateUpdateEvent voice-state-update-event]
  (when (.isLeaveEvent voice-state-update-event)
    (let [voice-state (.getCurrent voice-state-update-event)
          guild-id (-> voice-state .getGuildId .asString)]
      (log/info (format "Bot has been disconnected - removing audio manager & voice-connection entries for guild %s" guild-id))
      (player/handle-guild-disconnect guild-id))))

;; Useful for cleanup
(defn remove-all-command-definitions [^RestClient rest-client]
  (let [application-id (-> rest-client .getApplicationId .block)
        application-service (-> rest-client .getApplicationService)
        commands (-> application-service
                     (.getGlobalApplicationCommands application-id)
                     (.collectMap (util/as-function (fn [x] (.name x))))
                     (.block))]
    (run!
     (fn [[x ^ApplicationCommandData command-data]]
       (let [cmd-id (-> (.id command-data) (Long/parseLong))]
         (-> application-service
             (.deleteGlobalApplicationCommand application-id cmd-id)
             (.subscribe))))
     commands)))

(defn add-command-definitions [^RestClient rest-client]
  (let [application-id (-> rest-client .getApplicationId .block)
        application-service (-> rest-client .getApplicationService)]
    (run!
     (fn [[name {:keys [doc args] :as handler-info}]]
       (let [base-command-request (-> (ApplicationCommandRequest/builder)
                                      (.name name)
                                      (.description doc))
             args-as-options (mapv
                              (fn [{:keys [name doc required?]}]
                                (-> (ApplicationCommandOptionData/builder)
                                    (.name name)
                                    (.description doc)
                                    (.type (.getValue ApplicationCommandOption$Type/STRING))
                                    (.required required?)
                                    (.build)))
                              args)
             command-request (.build (reduce
                                      (fn [cmd-request option-data]
                                        (.addOption cmd-request option-data))
                                      base-command-request
                                      args-as-options))]
         (-> application-service
             (.createGlobalApplicationCommand application-id command-request)
             (.subscribe))
         (log/info (format "Slash commanded added for %s" name))))
     handlers/handlers)))

(defn handle-client [^GatewayDiscordClient client]
  ;; Logout client on shutdown
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. ^Runnable (fn [] (some-> client .logout .block))))

  ;; Add global application commands to client
  (add-command-definitions (.getRestClient client))

  ;; Handle events
  (let [on-login (-> client
                     (.on ReadyEvent (util/as-function on-bot-ready))
                     (.then))
        on-message (-> client
                       (.on MessageCreateEvent (util/as-function on-message))
                       (.then))
        on-chat-input (-> client
                          (.on ChatInputInteractionEvent (util/as-function on-chat-input))
                          (.then))
        on-voice-state-update (-> client
                                  (.on VoiceStateUpdateEvent (util/as-function on-voice-state-update))
                                  (.then))]
    (-> on-login
        (.and on-message)
        (.and on-chat-input)
        (.and on-voice-state-update))))

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
