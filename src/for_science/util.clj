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

(ns for-science.util
  (:require [clojure.tools.logging :as log]))

(defn human-readable-date-diff
  "Returns a string containing the human readable difference between two instants."
  [^java.time.Instant i1
   ^java.time.Instant i2]
  (format "%dd %dh %dm %d.%03ds" (.until i1 i2 (java.time.temporal.ChronoUnit/DAYS))
                                 (mod (.until i1 i2 (java.time.temporal.ChronoUnit/HOURS))     24)
                                 (mod (.until i1 i2 (java.time.temporal.ChronoUnit/MINUTES))   60)
                                 (mod (.until i1 i2 (java.time.temporal.ChronoUnit/SECONDS))   60)
                                 (mod (.until i1 i2 (java.time.temporal.ChronoUnit/MILLIS))  1000)))

(def ^:private units ["B" "KB" "MB" "GB" "TB" "PB" "EB" "ZB" "YB"])
(def ^:private ^java.text.DecimalFormat df (java.text.DecimalFormat. "#.##"))

(defn human-readable-size
  [size]
  (let [index (loop [size size
                     index 0]
                (if (< size 1024)
                  index
                  (recur (/ size 1024) (inc index))))]
    (str (.format df (/ size (Math/pow 1024 index))) (nth units index))))

(defn log-exception
  "Logs the given exception and (optional) message at ERROR level."
  ([^java.lang.Throwable e] (log-exception e nil))
  ([^java.lang.Throwable e msg]
   (let [extra (ex-data e)
         m     (case [(boolean msg) (boolean extra)]
                 [true  true]  (str msg "; data: " extra)
                 [true  false] msg
                 [false true]  (str "Data: " extra)
                 [false false] (if e (.getMessage e) "No exception information provided (this is probably a bug)"))]
     (log/error e m))))

(defn exit
  "Exits the program after printing the given message, and returns the given status code."
  ([]            (exit 0 nil))
  ([status-code] (exit status-code nil))
  ([status-code message]
   (when message
     (if (= 0 status-code)
       (println message)
       (binding [*out* *err*]
         (println message))))
   (flush)
   (shutdown-agents)
   (System/exit status-code)))
