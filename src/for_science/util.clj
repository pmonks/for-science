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
  (:require [clojure.string        :as s]
            [clojure.tools.logging :as log]
            [java-time             :as tm]))
(comment
(defn nth-fibonacci
  "Returns the nth fibonacci."
  [n]
  (loop [series [0 1]]
    (if (> (count series) n)
      (nth series n)
      (let [[n-1 n-2] (rseq series)]  ; We use rseq here as it is constant time on vectors (vs reverse, which is linear time)
        (recur (conj series (+' n-1 n-2)))))))

(defn parse-int
  "Parses a value (a string or numeric) into a Clojure integer (Java Long or BigInteger), returning nil if parsing failed."
  [x]
  (cond (integer?  x) x
        (string?   x) (try
                        (Long/parseLong (s/trim x))
                        (catch NumberFormatException nfe
                          nil))
        (float?    x) (int (Math/round ^Float x))
        (double?   x) (int (Math/round ^Double x))
        (rational? x) (parse-int (double x))
        :else         nil))

(defn getrn
  "Like get, but also replace nil values found in the map with the default value."
  [m k nf]
  (or (get m k nf) nf))

(defn mapfonk
  "Returns a new map where f has been applied to all of the keys of m."
  [f m]
  (when m
    (into {}
          (for [[k v] m]
            [(f k) v]))))

(defn mapfonv
  "Returns a new map where f has been applied to all of the values of m."
  [f m]
  (when m
    (into {}
          (for [[k v] m]
            [k (f v)]))))

(defn clojurise-json-key
  "Converts JSON string keys (e.g. \"fullName\") to Clojure keyword keys (e.g. :full-name)."
  [k]
  (keyword
    (s/replace
      (s/join "-"
              (map s/lower-case
                   (s/split k #"(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")))
      "_"
      "-")))

(defn replace-all
  "Takes a sequence of replacements, and applies all of them to the given string, in the order provided.  Each replacement in the sequence is a pair of values to be passed to clojure.string/replace (the 2nd and 3rd arguments)."
  [string replacements]
  (loop [s string
         f (first replacements)
         r (rest  replacements)]
    (if f
      (recur (s/replace s (first f) (second f))
             (first r)
             (rest  r))
      s)))

(defn to-ascii
  "Converts the given string to ASCII, mapping a small number of Unicode characters to their ASCII equivalents."
  [s]
  (replace-all s
               [[#"\p{javaWhitespace}" " "]     ; Whitespace
                [#"[–‑‒–—]"            "-"]     ; Hyphens / dashes
                [#"[^\p{ASCII}]+"      ""]]))   ; Strip everything else

(defn truncate
  "If s is longer than len, truncates it to len-1 and adds the ellipsis (…) character to the end."
  [s len]
  (if (> (count s) len)
    (str (subs s 0 (dec len)) "…")
    s))

(defmacro in-tz
  "Executes body (assumed to include java-time logic) within the given tzdata timezone (e.g. Americas/Los_Angeles)."
  [tz & body]
  `(tm/with-clock (tm/system-clock ~tz) ~@body))
)
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
