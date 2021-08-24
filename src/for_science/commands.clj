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

(ns for-science.commands
  (:require [clojure.string               :as s]
            [clojure.tools.logging        :as log]
            [sci.core                     :as sci]
            [discljord.formatting         :as df]
            [discljord-utils.message-util :as mu]
            [bot.commands                 :as cmd]
            [bot.config                   :as cfg]))

(def default-timeout-in-sec 2)

(def clojure-code-fence-regex #"(?s)```(clojure)??\s+(.*?)```")

(def code-prefix "(use 'clojure.repl)")  ; Prefix to all code evaluation
(def sci-opts    {})                     ; sci options map

(defn- eval-clj
  "Evaluates the given Clojure code, with a timeout on execution (default is 2 seconds). Result is a map which may contain these keys:

  :output any output sent to stdout or stderr
  :result the last result returned by the evaluated code
  :error  an error (either a string or a Throwable), if an error occurred"
  ([code] (eval-clj code default-timeout-in-sec))
  ([code timeout-in-sec]
   (when code
     (log/debug "Evaluating Clojure forms:" code)
     (let [result (try
                    (let [eval-result (future
                                        (try
                                          (let [sw     (java.io.StringWriter.)
                                                result (sci/binding [sci/out sw
                                                                     sci/err sw]
                                                         (pr-str (sci/eval-string (str code-prefix "\n" code) sci-opts)))]    ; Make sure we stringify the result inside sci/binding, to force de-lazying of the result of evaluating code
                                            (merge {:result result}
                                                   (when-let [output (when-not (s/blank? (str sw)) (str sw))] {:output output})))
                                          (catch Throwable t
                                            {:error t})))]
                      (deref eval-result
                             (* 1000 timeout-in-sec)
                             {:error (str "Execution terminated after " timeout-in-sec "s.")}))
                    (catch Throwable t
                      {:error t}))]
       (log/debug "Result:" result)
       result))))

(defn ^{:bot-command "clj"} clj-command!
  "Evaluates the body of the message as Clojure code, or, if the message contains clojure or unqualified code fences, combines and evaluates them (ignoring everything outside the code fences, thereby enabling 'literate' style messages)"
  [args event-data]
  (when-not (s/blank? args)
    (let [channel-id          (:channel-id event-data)
          clojure-code-fences (re-seq clojure-code-fence-regex args)
          result              (if clojure-code-fences
                                (eval-clj (s/join "\n" (map #(nth % 2) clojure-code-fences)))   ; 3rd group in the regex is the code
                                (eval-clj args))
          message             (if (:error result)
                                (str "```\n⚠️ " (:error result) "\n```")
                                (str (when (:output result) (str "Output:\n```\n" (:output result) "\n```\n"))
                                     "Result:\n```clojure\n" (:result result) "\n```"))]
      (mu/create-message! (:discord-message-channel cfg/config)
                          channel-id
                          :embed (assoc (cmd/embed-template)
                                        :description message)))))

(defn ^{:bot-command "move"} move-command!
  "Moves a conversation to the specified channel e.g. !move #memes"
  [args event-data]
  (when (not (mu/direct-message? event-data))   ; Only respond if the message was sent to a real channel in a server (i.e. not in a DM)
    (let [guild-id                (:guild-id event-data)
          channel-id              (:channel-id event-data)
          discord-message-channel (:discord-message-channel cfg/config)]
      (if (not (s/blank? args))
        (if-let [target-channel-id (second (re-find df/channel-mention args))]
          (if (not= channel-id target-channel-id)
            (let [move-message-id    (:id event-data)
                  _                  (mu/delete-message! discord-message-channel channel-id move-message-id)   ; Don't delete the original message unless we've validated everything
                  target-message-id  (:id (mu/create-message! discord-message-channel
                                                              target-channel-id
                                                              :embed (assoc (cmd/embed-template)
                                                                            :description (str "Continuing the conversation from " (mu/channel-link channel-id) "..."))))
                  target-message-url (mu/message-url guild-id target-channel-id target-message-id)
                  source-message-id  (:id (mu/create-message! discord-message-channel
                                                              channel-id
                                                              :embed (assoc (cmd/embed-template)
                                                                            :description (str "Let's continue this conversation in " (mu/channel-link target-channel-id) " ([link](" target-message-url "))."))))
                  source-message-url (mu/message-url guild-id channel-id source-message-id)]
              (mu/edit-message! discord-message-channel
                                target-channel-id
                                target-message-id
                                :embed (assoc (cmd/embed-template)
                                              :description (str "Continuing the conversation from " (mu/channel-link channel-id)  " ([link](" source-message-url "))..."))))
            (log/info "Cannot move a conversation to the same channel."))
          (log/warn "Could not find target channel in move command."))
        (log/warn "move-command! arguments missing a target channel.")))))
