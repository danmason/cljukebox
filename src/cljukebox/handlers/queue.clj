(ns cljukebox.handlers.queue
  (:require [cljukebox.player :as player]
            [cljukebox.util :as util])
  (:import java.lang.NumberFormatException))

(defn calculate-queue-length [track-queue]
  (->> track-queue
       (map (fn [track] (-> track .getInfo .length)))
       (reduce +)
       util/millis->time-str))

(defn create-track-field [idx track]
  (let [{:keys [title author length] :as track-info} (player/track->map track)]
    {:name (format "%d: %s" idx title)
     :value (format "**Author**: %s | **Length**: %s" author length)}))

(defn output-audio-queue [{:keys [message-channel]} {:keys [page-number]} {:keys [scheduler]}]
  (try
    (let [track-queue (.queue scheduler)
          page-number (or (some-> page-number Long/parseLong) 1)
          track-offset (* (- page-number 1) 20)
          track-queue-page (take 20 (drop track-offset track-queue))
          page-indexes (range (+ track-offset 1)
                              (+ track-offset (count track-queue-page) 1))]
      (util/send-embed message-channel {:title "Bot Queue"
                                        :fields (concat [{:name "Total Tracks"
                                                          :value (str (count track-queue))
                                                          :inline true}
                                                         {:name "Queue Length"
                                                          :value (calculate-queue-length track-queue)
                                                          :inline true}
                                                         {:name "Page"
                                                          :value (str page-number)
                                                          :inline true}]
                                                        (map create-track-field page-indexes track-queue-page))}))
    (catch NumberFormatException e
      (util/send-message message-channel "Invalid page number provided - should be an integer, e.g `queue 2`"))))

(defn audio-queue
  ([{:keys [content] :as data}]
   (let [page-number (util/get-arguments content)]
     (audio-queue data {:page-number page-number})))
  ([{:keys [message-channel guild-id] :as data} opts]
   (if-let [guild-manager (player/get-guild-audio-manager guild-id)]
     (output-audio-queue data opts guild-manager)
     (util/missing-audio-manager-message message-channel))))

(def handler-data
  {:doc "Outputs the current player queue for the server"
   :usage-str "queue"
   :args [{:name "page-number"
           :doc "Which page of the queue to display (each page contains 20 results)"
           :required? false}]
   :handler-fn audio-queue})
