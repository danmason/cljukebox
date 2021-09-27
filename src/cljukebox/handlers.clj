(ns cljukebox.handlers
  (:require [clojure.string :as string]
            [cljukebox.util :as util]
            [cljukebox.handlers
             [prefix :as prefix]
             [play :as play]
             [queue :as queue]
             [leave :as leave]
             [skip :as skip]
             [loop :as loop]]))

(def base-handlers
  {"prefix" prefix/handler-data
   "play" play/handler-data
   "queue" queue/handler-data
   "leave" leave/handler-data
   "skip" skip/handler-data
   "loop" loop/handler-data})

(defn help-handler [{:keys [message-channel guild-id content] :as data}]
  (let [prefix (util/get-prefix guild-id)
        [command] (rest (string/split content #" "))]
    (if command
      (if-let [{:keys [doc usage-str]} (get base-handlers command)]
        (util/send-embed message-channel {:title command
                                          :description doc
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
  (assoc base-handlers "help" {:handler-fn help-handler}))

(defn get-handler-fn [content prefix]
  (some (fn [[k v]]
          (when (string/starts-with? content (str prefix k))
            (:handler-fn v))) handlers))
