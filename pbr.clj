;
; Copyright © 2021 Peter Monks
;
; This Source Code Form is subject to the terms of the Mozilla Public
; License, v. 2.0. If a copy of the MPL was not distributed with this
; file, You can obtain one at https://mozilla.org/MPL/2.0/.
;
; SPDX-License-Identifier: MPL-2.0
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
               :licenses         [:license   {:name "MPL-2.0" :url "https://www.mozilla.org/en-US/MPL/2.0/"}]
               :developers       [:developer {:id "pmonks" :name "Peter Monks" :email "pmonks+forscience@gmail.com"}]
               :scm              {:url "https://github.com/pmonks/for-science" :connection "scm:git:git://github.com/pmonks/for-science.git" :developer-connection "scm:git:ssh://git@github.com/pmonks/for-science.git"}
               :issue-management {:system "github" :url "https://github.com/pmonks/for-science/issues"}}))
