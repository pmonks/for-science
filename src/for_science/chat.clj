;
; Copyright © 2021 Peter Monks
;
; Licensed under the Apache License, Version 2.0 (the "License");
; you may not use this file except in compliance with the License.
; You may obtain a copy of the License at
;
;     http://www.apache.org/licenses/LICENSE-2.0
;
; Unless required by applicable law or agreed to in writing, software
; distributed under the License is distributed on an "AS IS" BASIS,
; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
; See the License for the specific language governing permissions and
; limitations under the License.
;
; SPDX-License-Identifier: Apache-2.0
;

(ns for-science.chat
  (:require [clojure.string           :as s]
            [clojure.repl             :as repl]
            [clojure.tools.logging    :as log]
            [java-time                :as tm]
            [sci.core                 :as sci]
            [discljord.formatting     :as df]
            [for-science.util         :as u]
            [for-science.message-util :as mu]
            [for-science.config       :as cfg]))

(def prefix "!")

(def timeout-in-sec 2)

(def clojure-code-fence-regex #"(?s)```(clojure)??\s+(.*?)```")

(def code-prefix "(use 'clojure.repl)")  ; Prefix to all code evaluation
(def sci-opts    {})                     ; sci options map

(defn- eval-clj
  "Evaluates the given Clojure code, with a timeout on execution. Result is a map which may contain these keys:

  :stdout any "
  [code]
  (when code
    (log/debug "Evaluating Clojure forms:" code)
    (let [result (future (try
                           (let [sw     (java.io.StringWriter.)
                                 result (sci/binding [sci/out sw
                                                      sci/err sw]
                                          (print-str (sci/eval-string (str code-prefix "\n" code) sci-opts)))]    ; Make sure we stringify the result inside sci/binding, to force de-lazying of the result of evaluating code
                             (merge {:result result}
                                    (when-let [output (when-not (s/blank? (str sw)) (str sw))] {:output output})))
                           (catch Throwable t
                             {:error t})))]
      (try
        (deref result
               (* 1000 timeout-in-sec)
               {:error (str "Execution terminated after " timeout-in-sec "s.")})
        (catch Throwable t
          {:error t})))))

(defn clj-command!
  "Evaluates the body of the message as Clojure code, or, if the message contains clojure or unqualified code fences, combines and evaluates them (ignoring everything outside the code fences, thereby enabling 'literate' style messages)"
  [args event-data]
  (when-not (s/blank? args)
    (let [channel-id          (:channel-id event-data)
          clojure-code-fences (re-seq clojure-code-fence-regex args)
          result              (if clojure-code-fences
                                (eval-clj (s/join "\n" (map #(nth % 2) clojure-code-fences)))   ; 3rd group in the regex is the code
                                (eval-clj args))
          _                   (log/debug (str "Evaluation result: " result))
          message             (if (:error result)
                                (str "```\n⚠️ " (:error result) "\n```")
                                (str (when (:output result) (str "Output:\n```\n" (:output result) "\n```\n")) "Result:\n```clojure\n" (:result result) "\n```"))]
      (mu/create-message! cfg/discord-message-channel
                          channel-id
                          :embed (assoc (mu/embed-template)
                                        :description message)))))

(defn move-command!
  "Moves a conversation to the specified channel"
  [args event-data]
  (when (not (mu/direct-message? event-data))   ; Only respond if the message was sent to a real channel in a server (i.e. not in a DM)
    (let [guild-id   (:guild-id event-data)
          channel-id (:channel-id event-data)
          message-id (:id event-data)]
      (mu/delete-message! cfg/discord-message-channel channel-id message-id)
      (if (not (s/blank? args))
        (if-let [target-channel-id (second (re-find df/channel-mention args))]
          (if (not= channel-id target-channel-id)
            (let [target-message-id  (:id (mu/create-message! cfg/discord-message-channel
                                                              target-channel-id
                                                              :embed (assoc (mu/embed-template)
                                                                            :description (str "Continuing the conversation from " (mu/channel-link channel-id) "..."))))
                  target-message-url (mu/message-url guild-id target-channel-id target-message-id)
                  source-message-id  (:id (mu/create-message! cfg/discord-message-channel
                                                              channel-id
                                                              :embed (assoc (mu/embed-template)
                                                                            :description (str "Let's continue this conversation in " (mu/channel-link target-channel-id) " ([link](" target-message-url "))."))))
                  source-message-url (mu/message-url guild-id channel-id source-message-id)]
              (mu/edit-message! cfg/discord-message-channel
                                target-channel-id
                                target-message-id
                                :embed (assoc (mu/embed-template)
                                              :description (str "Continuing the conversation from " (mu/channel-link channel-id)  " ([link](" source-message-url "))..."))))
            (log/info "Cannot move a conversation to the same channel."))
          (log/warn "Could not find target channel in move command."))
        (log/warn "move-command! arguments missing a target channel.")))))

(defn privacy-command!
  "Provides a link to the for-science privacy policy"
  [_ event-data]
  (mu/create-message! cfg/discord-message-channel
                      (:channel-id event-data)
                      :embed (assoc (mu/embed-template)
                                    :description "[for-science's privacy policy is available here](https://github.com/pmonks/for-science/blob/main/PRIVACY.md).")))

(defn status-command!
  "Provides technical status of for-science"
  [_ event-data]
  (let [now (tm/instant)]
    (mu/create-message! cfg/discord-message-channel
                        (:channel-id event-data)
                        :embed (assoc (mu/embed-template)
                                      :title "for-science Status"
                                      :fields [
                                        {:name "Running for"            :value (str (u/human-readable-date-diff cfg/boot-time now))}
                                        {:name "Built at"               :value (str (tm/format :iso-instant cfg/built-at) (if cfg/git-url (str " from [" cfg/git-tag "](" cfg/git-url ")") ""))}

                                        ; Table of fields here
                                        {:name "Clojure"                :value (str "v" (clojure-version)) :inline true}
                                        {:name "JVM"                    :value (str (System/getProperty "java.vm.vendor") " v" (System/getProperty "java.vm.version") " (" (System/getProperty "os.name") "/" (System/getProperty "os.arch") ")") :inline true}
                                        ; Force a newline (Discord is hardcoded to show 3 fields per line), by using Unicode zero width spaces (empty/blank strings won't work!)
                                        {:name "​"                       :value "​" :inline true}
                                        {:name "Heap memory in use"     :value (u/human-readable-size (.getUsed (.getHeapMemoryUsage (java.lang.management.ManagementFactory/getMemoryMXBean)))) :inline true}
                                        {:name "Non-heap memory in use" :value (u/human-readable-size (.getUsed (.getNonHeapMemoryUsage (java.lang.management.ManagementFactory/getMemoryMXBean)))) :inline true}
                                      ]))))

(defn gc-command!
  "Requests that the JVM perform a GC cycle."
  [_ event-data]
  (System/gc)
  (mu/create-message! cfg/discord-message-channel
                      (:channel-id event-data)
                      :content "Garbage collection requested."))

(defn set-logging-command!
  "Sets the log level, optionally for the given logger (defaults to 'for-science')."
  [args event-data]
  (let [[level logger] (s/split args #"\s+")]
    (if level
      (do
        (cfg/set-log-level! level (if logger logger "for-science"))
        (mu/create-message! cfg/discord-message-channel
                            (:channel-id event-data)
                            :content (str "Logging level " (s/upper-case level) " set" (if logger (str " for logger '" logger "'") "for logger 'for-science'") ".")))
      (mu/create-message! cfg/discord-message-channel
                            (:channel-id event-data)
                            :content "Logging level not provided; must be one of: ERROR, WARN, INFO, DEBUG, TRACE"))))

(defn debug-logging-command!
  "Enables debug logging, which turns on TRACE for 'discljord' and DEBUG for 'for-science'."
  [_ event-data]
  (cfg/set-log-level! "TRACE" "discljord")
  (cfg/set-log-level! "DEBUG" "for-science")
  (mu/create-message! cfg/discord-message-channel
                      (:channel-id event-data)
                      :content "Debug logging enabled (TRACE for 'discljord' and DEBUG for 'for-science'."))

(defn reset-logging-command!
  "Resets all log levels to their configured defaults."
  [_ event-data]
  (cfg/reset-logging!)
  (mu/create-message! cfg/discord-message-channel
                      (:channel-id event-data)
                      :content "Logging configuration reset."))


; Table of "public" commands; those that can be used in any channel, group or DM
(def public-command-dispatch-table
  {"clj"  #'clj-command!
   "move" #'move-command!})

(declare help-command!)

; Table of "private" commands; those that can only be used in a DM channel
(def private-command-dispatch-table
  {"help"    #'help-command!
   "privacy" #'privacy-command!})

(def secret-command-dispatch-table
  {"status"       #'status-command!
   "gc"           #'gc-command!
   "setlogging"   #'set-logging-command!
   "debuglogging" #'debug-logging-command!
   "resetlogging" #'reset-logging-command!})

(defn help-command!
  "Displays this help message"
  [_ event-data]
  (mu/create-message! cfg/discord-message-channel
                      (:channel-id event-data)
                      :embed (assoc (mu/embed-template)
                                    :description (str "I understand the following commands in any channel or DM:\n"
                                                      (s/join "\n" (map #(str " • **`" prefix (key %) "`** - " (:doc (meta (val %))))
                                                                        (sort-by key public-command-dispatch-table)))
                                                      "\n\nAnd the following commands only in a DM:\n"
                                                      (s/join "\n" (map #(str " • **`" prefix (key %) "`** - " (:doc (meta (val %))))
                                                                        (sort-by key private-command-dispatch-table)))))))

; Responsive fns
(defmulti handle-discord-event
  "Discord event handler"
  (fn [event-type _] event-type))

; Default Discord event handler (noop)
(defmethod handle-discord-event :default
  [_ _])

(defmethod handle-discord-event :message-create
  [_ event-data]
  ; Only respond to messages sent from a human
  (when (mu/human-message? event-data)
    (future    ; Spin off the actual processing, so we don't clog the Discord event queue
      (try
        (let [content (s/triml (:content event-data))]
          (if (s/starts-with? content prefix)
            ; Parse the requested command and call it, if it exists
            (let [command-and-args  (s/split content #"\s+" 2)
                  command           (s/lower-case (subs (s/trim (first command-and-args)) (count prefix)))
                  args              (second command-and-args)]
              (if-let [public-command-fn (get public-command-dispatch-table command)]
                (do
                  (log/debug (str "Calling public command fn for '" command "' with args '" args "'."))
                  (public-command-fn args event-data))
                (when (mu/direct-message? event-data)
                  (if-let [private-command-fn (get private-command-dispatch-table command)]
                    (do
                      (log/debug (str "Calling private command fn for '" command "' with args '" args "'."))
                      (private-command-fn args event-data))
                    (if-let [secret-command-fn (get secret-command-dispatch-table command)]
                      (do
                        (log/debug (str "Calling secret command fn for '" command "' with args '" args "'."))
                        (secret-command-fn args event-data))
                      (help-command! nil event-data))))))   ; If the requested private command doesn't exist, provide help
            ; If any unrecognised message was sent to a DM channel, provide help
            (when-not (:guild-id event-data)
              (help-command! nil event-data))))
        (catch Exception e
          (u/log-exception e))))))
