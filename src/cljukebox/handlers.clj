(ns cljukebox.handlers
  (:require [clojure.string :as string]
            [cljukebox.util :as util]
            [cljukebox.handlers
             [prefix :as prefix]
             [play :as play]
             [queue :as queue]
             [leave :as leave]
             [skip :as skip]
             [loop :as loop]
             [clear :as clear]
             [nowplaying :as np]]))

(def base-handlers
  {"prefix" prefix/handler-data
   "play" play/handler-data
   "queue" queue/handler-data
   "leave" leave/handler-data
   "skip" skip/handler-data
   "loop" loop/handler-data
   "clear" clear/handler-data
   "nowplaying" np/handler-data})

(defn help-handler
  ([{:keys [message-channel content] :as data}]
   (let [command (util/get-arguments content)]
     (help-handler data {:command command})))
  ([{:keys [message-channel] :as data} {:keys [command] :as opts}]
   (if command
     (if-let [{:keys [long-doc doc usage-str]} (get base-handlers command)]
       (util/send-embed message-channel {:title command
                                         :description (or long-doc doc)
                                         :fields [{:name "Usage Example"
                                                   :value (format "`%s`" usage-str)}]})
       (util/send-message message-channel (format "*%s* is not an existing command" command)))
     (util/send-embed message-channel {:title "Help Menu"
                                       :description "For more information about specific commands, use `help <command>`"
                                       :fields (mapv (fn [[k {:keys [doc]}]]
                                                       {:name k
                                                        :value doc})
                                                     base-handlers)}))))

(def handlers
  (assoc base-handlers "help" {:doc "Outputs information about the various commands on the bot"
                               :args [{:name "command"
                                       :required? false
                                       :doc "Specific command you wish to ask for information on"}]
                               :handler-fn help-handler}))

(defn get-handler-fn [content prefix]
  (some (fn [[k v]]
          (when (string/starts-with? content (str prefix k))
            (:handler-fn v))) handlers))
