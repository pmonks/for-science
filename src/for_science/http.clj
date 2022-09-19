;
; Copyright © 2022 Peter Monks
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

(ns for-science.http
  (:require [clojure.tools.logging :as log]
            [mount.core            :as mnt :refer [defstate]]
            [java-time             :as tm]
            [org.httpkit.server    :as http]
            [bot.config            :as cfg]
            [discljord-utils.util  :as u]))

(defn health-check-handler
  [_]
  {:status  200
   :headers {"Content-Type" "text/html"}
   :body    (str "<!DOCTYPE html>
<html>
  <head><title>for-science</title></head>
  <body><a href=\"https://github.com/pmonks/for-science\">for-science</a> Discord Bot up for " (u/human-readable-date-diff cfg/boot-time (tm/instant)) ".</body>
</html>")})

(def ^:private port (if-let [port (u/parse-int (System/getenv "PORT"))] port 8080))

(declare  http-health-check-server)
(defstate http-health-check-server
  :start (do
           (log/info (str "Starting health check HTTP server on port " port))
           (http/run-server health-check-handler {:port port :legacy-return-value? false}))
  :stop  (when-let [stopping (http/server-stop! http-health-check-server)] @stopping))
