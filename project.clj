(defproject keepass-converter "0.1.0-SNAPSHOT"
  :description "a command-line utility to convert KeePass export files to CSV format"
  :url "http://github.com/propan/keepass-converter"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [enlive "1.1.5"]
                 [org.clojure/data.csv "0.1.2"]
                 [org.clojure/tools.cli "0.3.1"]]
  :main ^:skip-aot keepass-converter.app
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
