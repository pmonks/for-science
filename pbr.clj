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

(defn set-opts
  [opts]
  (assoc opts
         :lib              'org.github.pmonks/for-science
         :version          (format "1.0.%s" (.format (java.text.SimpleDateFormat. "yyyyMMdd") (java.util.Date.)))
         :prod-branch      "release"
         :uber-file        "./target/for-science-standalone.jar"
         :main             'bot.main
         :deploy-info-file "./resources/build-info.edn"
         :write-pom        true
         :pom {:description      "A small Discord bot that you can send Clojure code to, to experiment with the language, demonstrate core language principles, or just mess about."
               :url              "https://github.com/pmonks/for-science"
               :licenses         [:license   {:name "Apache-2.0" :url "http://www.apache.org/licenses/LICENSE-2.0.html"}]
               :developers       [:developer {:id "pmonks" :name "Peter Monks" :email "pmonks+forscience@gmail.com"}]
               :scm              {:url "https://github.com/pmonks/for-science" :connection "scm:git:git://github.com/pmonks/for-science.git" :developer-connection "scm:git:ssh://git@github.com/pmonks/for-science.git"}
               :issue-management {:system "github" :url "https://github.com/pmonks/for-science/issues"}}))
