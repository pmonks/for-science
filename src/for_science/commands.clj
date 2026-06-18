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
            [embroidery.api               :as e]
            [rencg.api                    :as re]
            [discljord.formatting         :as df]
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

(defn ^{:bot-command "clj"} clj-command!
  "Evaluates the body of the message as Clojure code, or, if the message contains clojure, clj, or unqualified code fences, combines and evaluates them (ignoring everything outside the code fences, thereby enabling 'literate' style messages)"
  [args event-data]
  (when-not (s/blank? args)
    (let [channel-id   (:channel-id event-data)
          clojure-code (s/trim
                         (if-let [clojure-snippets (re/re-seq clojure-code-fence-regex args)]
                           (s/join "\n" (filter #(not (s/blank? %)) (map #(get % "source") clojure-snippets)))
                           args))
          eval-result  (eval-clj clojure-code)
          message      (if (:error eval-result)
                         (str "```\n⚠️ " (:error eval-result) "\n```")
                         (str (when (:output eval-result) (str "Output:\n```\n" (:output eval-result) "\n```\n"))
                              "Result:\n```clojure\n" (:result eval-result) "\n```"))]
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
