;
; Copyright © 2021 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
;

(ns for-science.commands
  (:require [clojure.core                 :as c]
            [clojure.string               :as s]
            [clojure.math                 :as m]   ; Ignore clj-kondo warning: this is required for sci initialisation
            [clojure.tools.logging        :as log]
            [sci.core                     :as sci]
            [sci.impl.utils               :as sciiu]
            [slash.command                :as slash-cmd]
            [slash.command.structure      :as stc]
            [slash.component.structure    :as cmps]
            [embroidery.api               :as e]
            [rencg.api                    :as re]
            [discljord-utils.util         :as u]
            [discljord-utils.message-util :as mu]
            [bot.commands                 :as cmd]
            [bot.config                   :as cfg]))

(def ^:private default-timeout-in-sec 2)
(def ^:private maximum-output-length  512)

(def ^:private clojure-code-fence-regex #"(?is)```(?:(?:clojure|clj)\s+)?(?<source>.*?)```")

; See https://github.com/babashka/sci/issues/952
; From https://github.com/babashka/sci.configs/blob/main/src/sci/configs/clojure_1_11.cljc
(def ^:private clojure-core-namespace-extras-1-11
  {'abs           (sci/copy-var c/abs           sciiu/clojure-core-ns)
   'NaN?          (sci/copy-var c/NaN?          sciiu/clojure-core-ns)
   'infinite?     (sci/copy-var c/infinite?     sciiu/clojure-core-ns)
   'parse-double  (sci/copy-var c/parse-double  sciiu/clojure-core-ns)
   'parse-long    (sci/copy-var c/parse-long    sciiu/clojure-core-ns)
   'parse-boolean (sci/copy-var c/parse-boolean sciiu/clojure-core-ns)
   'parse-uuid    (sci/copy-var c/parse-uuid    sciiu/clojure-core-ns)
   'random-uuid   (sci/copy-var c/random-uuid   sciiu/clojure-core-ns)
   'update-keys   (sci/copy-var c/update-keys   sciiu/clojure-core-ns)
   'update-vals   (sci/copy-var c/update-vals   sciiu/clojure-core-ns)
   'iteration     (sci/copy-var c/iteration     sciiu/clojure-core-ns)})

(def ^:private math       (sci/create-ns 'clojure.math))
(def ^:private math-ns    (sci/copy-ns clojure.math math))
(def ^:private namespaces {'clojure.core clojure-core-namespace-extras-1-11 'clojure.math math-ns})

(def ^:private code-prefix "(use 'clojure.repl)")                ; Prefix to all code evaluation
(def ^:private sci-ctx     (sci/init {:namespaces namespaces}))  ; sci context

(defn- truncate-string
  "Truncates a string to our maximum allowed output length, appending"
  [^String s]
  (when s
    (if (>= (count s) maximum-output-length)
      (str (subs s 0 maximum-output-length) "...and more")
      s)))

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
                    (let [f           (e/future*
                                        (try
                                          (let [sw     (java.io.StringWriter.)
                                                result (sci/binding [sci/out sw
                                                                     sci/err sw]
                                                         (truncate-string (pr-str (sci/eval-string* sci-ctx (str code-prefix "\n" code)))))]    ; Make sure we stringify the result inside sci/binding, to force de-lazying of the result of evaluating code
                                            (merge {:result result}
                                                   (when-let [output (when-not (s/blank? (str sw)) (truncate-string (str sw)))] {:output output})))
                                          (catch Throwable t
                                            {:error t})))
                          eval-result (deref f
                                        (* 1000 timeout-in-sec)
                                        {:error (str "Execution terminated after " timeout-in-sec "s.")})]
                      (when-not (future-done? f) (future-cancel f))
                      eval-result)
                    (catch Throwable t
                      {:error t}))]
       (log/debug "Result:" result)
       result))))

(def ^:private max-displayed-code-length 1000)
(def ^:private ephemeral-flag               64)   ; Discord message flag: only visible to the user who invoked the command

(defn- extract-code
  "Extracts the Clojure code from `text`: if it contains clojure, clj or unqualified code fences, combines their contents (ignoring everything outside the fences, thereby enabling 'literate' style messages), otherwise returns `text` as is."
  [text]
  (s/trim
    (if-let [clojure-snippets (re/re-seq clojure-code-fence-regex text)]
      (s/join "\n" (filter #(not (s/blank? %)) (map #(get % "source") clojure-snippets)))
      text)))

(defn- format-eval-result
  [eval-result]
  (if (:error eval-result)
    (str "```\n⚠️ " (:error eval-result) "\n```")
    (str (when (:output eval-result) (str "Output:\n```\n" (:output eval-result) "\n```\n"))
         "Result:\n```clojure\n" (:result eval-result) "\n```")))

(defn- truncate
  [s max-length]
  (if (> (count s) max-length)
    (str (subs s 0 max-length) "…")
    s))

(defn ^{:bot-command "clj"} clj-command!
  "Evaluates the body of the message as Clojure code, or, if the message contains clojure, clj, or unqualified code fences, combines and evaluates them (ignoring everything outside the code fences, thereby enabling 'literate' style messages)"
  [args event-data]
  (when-not (s/blank? args)
    (mu/create-message! (:discord-message-channel cfg/config)
                        (:channel-id event-data)
                        :embed (assoc (cmd/embed-template)
                                      :description (format-eval-result (eval-clj (extract-code args)))))))

(def slash-commands
  [(stc/command "clj"
                "Evaluates Clojure code"
                :options [(stc/option "code" "The Clojure code to evaluate" :string :required true)])
   (stc/command "move"
                "Moves a conversation to the specified channel"
                :options [(stc/option "channel" "The channel to continue the conversation in" :channel
                                      :channel-types [:guild-text]
                                      :required true)])])

(defn- reply-privately!
  "Replies to the interaction with a message that only the invoking user can see."
  [interaction-id interaction-token text]
  (mu/create-interaction-response! (:discord-message-channel cfg/config)
                                   interaction-id
                                   interaction-token
                                   4
                                   :data {:embeds [(assoc (cmd/embed-template) :description text)]
                                          :flags  ephemeral-flag}))

(defn- fail-privately!
  "Replaces the response with a message that only the invoking user can see, telling them that something went wrong."
  [interaction-token]
  (try
    (let [discord-message-channel (:discord-message-channel cfg/config)
          application-id          (:discord-application-id cfg/config)]
      (mu/delete-original-interaction-response! discord-message-channel application-id interaction-token)
      (mu/create-followup-message! discord-message-channel
                                   application-id
                                   interaction-token
                                   :embeds [(assoc (cmd/embed-template) :description "⚠️ Something went wrong while processing your command. Please try again later.")]
                                   :flags  ephemeral-flag))
    (catch Exception e
      (u/log-exception e))))

(slash-cmd/defhandler clj-slash-command
  ["clj"]
  {:keys [id token]}
  [code]
  (let [discord-message-channel (:discord-message-channel cfg/config)]
    ; evaluation can take a couple of seconds but discord requires an answer within 3 seconds, so send a thinking... response and edit it afterwards
    (mu/create-interaction-response! discord-message-channel id token 5)
    (e/future*
      (try
        (mu/edit-original-interaction-response!
          discord-message-channel
          (:discord-application-id cfg/config)
          token
          :embeds [(assoc (cmd/embed-template) 
                          :description (str "Code:\n```clojure\n" (truncate code max-displayed-code-length) "\n```\n"
                                            (format-eval-result (eval-clj (extract-code code)))))])
        (catch Exception e
          (u/log-exception e)
          (fail-privately! token))))))

(slash-cmd/defhandler move-slash-command
  ["move"]
  {:keys [id token channel-id guild-id]}
  [channel]
  (cond
    (nil? guild-id)        (reply-privately! id token "This command can only be used in a server channel.")
    (= channel channel-id) (reply-privately! id token "Can't move the conversation to the same channel.")
    :else
    (let [discord-message-channel (:discord-message-channel cfg/config)
          application-id          (:discord-application-id cfg/config)]
      (mu/create-interaction-response! discord-message-channel id token 5)
      (try
        (let [target-message-id  (:id (mu/create-message! discord-message-channel
                                                          channel
                                                          :embed (assoc (cmd/embed-template)
                                                                        :description (str "Continuing the conversation from " (mu/channel-link channel-id) "..."))))
              target-message-url (mu/message-url guild-id channel target-message-id)
              source-message-id  (:id (mu/edit-original-interaction-response!
                                        discord-message-channel
                                        application-id
                                        token
                                        :embeds     [(assoc (cmd/embed-template)
                                                            :description (str "Let's continue this conversation in " (mu/channel-link channel) " ([link](" target-message-url "))."))]
                                        :components [(cmps/action-row (cmps/link-button target-message-url :label "Link"))]))
              source-message-url (mu/message-url guild-id channel-id source-message-id)]
          (mu/edit-message! discord-message-channel
                            channel
                            target-message-id
                            :embed      (assoc (cmd/embed-template)
                                               :description (str "Continuing the conversation from " (mu/channel-link channel-id) " ([link](" source-message-url "))..."))
                            :components [(cmps/action-row (cmps/link-button source-message-url :label "Link"))]))
        (catch Exception e
          (u/log-exception e)
          (fail-privately! token))))))

(slash-cmd/defpaths slash-command-paths
  clj-slash-command
  move-slash-command)
