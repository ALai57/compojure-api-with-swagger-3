(defproject compojure-api-with-swagger-3.x "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cheshire "5.10.0"]
                 [metosin/compojure-api "2.0.0-alpha31"]
                 [ring "1.8.0"]
                 [http-kit "2.3.0"]]
  :managed-dependencies [[metosin/ring-swagger-ui "3.25.3"]]
  :main ^:skip-aot compojure-api-with-swagger-3.x
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
