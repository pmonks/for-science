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

(ns for-science.main
  (:require [CLJ-2253]
            [for-science.config    :as cfg]
            [clojure.string        :as s]
            [clojure.java.io       :as io]
            [clojure.tools.cli     :as cli]
            [clojure.tools.logging :as log]
            [mount.core            :as mnt]
            [java-time             :as tm]
            [discljord.events      :as de]
            [discljord-utils.util  :as u]
            [for-science.chat      :as chat])
  (:gen-class))

(def ^:private cli-opts
  [["-c" "--config-file FILE" "Path to configuration file (defaults to 'config.edn' in the classpath)"
    :validate [#(.exists (io/file %)) "Must exist"
               #(.isFile (io/file %)) "Must be a file"]]
   ["-h" "--help"]])

(defn usage
  [options-summary]
  (s/join
    \newline
    ["Runs the for-science Discord bot."
     ""
     "Usage: for-science [options]"
     ""
     "Options:"
     options-summary
     ""]))

(defn -main
  "Runs the for-science Discord bot."
  [& args]
  (try
    (log/info "Starting for-science on Clojure" (clojure-version) "/" (System/getProperty "java.vm.vendor") "JVM" (System/getProperty "java.vm.version") (str "(" (System/getProperty "os.name") "/" (System/getProperty "os.arch") ")"))
    (log/info (str "Released at " (tm/format :iso-instant cfg/built-at) (when cfg/git-url (str " from " cfg/git-url))))
    (let [{:keys [options errors summary]} (cli/parse-opts args cli-opts)]
      (cond
        (:help options) (u/exit 0 (usage summary))
        errors          (u/exit 1 (str "The following errors occurred while parsing the command line:\n\n"
                                       (s/join \newline errors))))

      ; Start the bot
      (mnt/with-args options)
      (mnt/start)
      (log/info "for-science started")
      (de/message-pump! cfg/discord-event-channel chat/handle-discord-event))   ; This must go last, as it blocks
    (catch Exception e
      (u/log-exception e)
      (u/exit -1)))
  (u/exit))
