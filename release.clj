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

(ns release
  "Release script for for-science (note: POSIX specific, YMMV on Windows).

For more information, run:

clojure -A:deps -T:release help/doc"
  (:require [org.pmonks.pbr :as pbr]
            [build          :as b]))

(defn check
  "Check that a release can be done from the current directory."
  [opts]
  (-> opts
      (b/set-opts)
      (b/ci)
      (pbr/check-release)))

(defn release
  "Release a new version of the bot."
  [opts]
  (-> opts
      (b/set-opts)
      (pbr/release)))
