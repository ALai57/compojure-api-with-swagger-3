(ns compojure-api-with-swagger-3.x
  (:gen-class)
  (:require [clojure.spec.alpha :as s]

            [org.httpkit.server :as httpkit]

            [cheshire.core :as json]

            [compojure.api.sweet :refer [api context GET POST]]
            [compojure.api.middleware :as mw]
            [compojure.api.swagger :as swag]
            [compojure.api.request :as req]

            [ring.util.http-response :refer :all]

            [ring.middleware.reload :refer [wrap-reload]]

            [ring.swagger.common :as rsc]
            [ring.swagger.middleware :as rsm]
            [ring.swagger.core :as swagger]
            [ring.swagger.swagger-ui :as swagger-ui]
            [ring.swagger.swagger2 :as swagger2]

            [spec-tools.swagger.core]
            ))

;; Define a spec that we will try to use for API documentation
;; This isn't working very well right now, because coercion between spec and
;; swagger seems busted
(s/def ::first_name string?)
(s/def ::last_name string?)
(s/def ::username string?)
(s/def ::user (s/keys :req-un [::first_name
                               ::last_name
                               ::username]))

;; Some examples:
;;   What a valid openapi 3 document looks like:
;;     https://idratherbewriting.com/learnapidoc/docs/rest_api_specifications/openapi_openweathermap.yml
;;   How to add examples to swagger UI:
;;     https://swagger.io/docs/specification/adding-examples/

(def app
  (-> {;; :exceptions nil
       :swagger {:swagger "2.0"
                 ;; This does not work
                 :openapi "3.0.2"
                 :ui "/swagger"
                 :coercion :spec
                 :spec "/swagger.json"
                 :data {:info {:title "Testing Swagger 3.x"
                               :description "A playground for experimenting"}
                        :tags [{:name "api", :description "My awesome API"}]}}}
      (api

       ;; `context` is here so that we can specify the coercion strategy. I
       ;; haven't been able to get clojure.specs to work without this here
       (context "/test" []

         ;; This is the coercion that is currently not working nicely. We may
         ;; need to write a custom coercion to support Spec, or just do more
         ;; research? In any case, swagger gets very mad when you try to insert
         ;; a schema under the "contents" header, as specified by the swagger
         ;; documentation here:
         ;; https://swagger.io/docs/specification/adding-examples/
         ;;:coercion :spec

         ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
         ;; BEGIN: This is my playground, ignore all this
         ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
         (GET "/" req
           :swagger
           {:summary "Test endpoint"
            :produces #{"application/json"}
            :responses {200 {:description "The article that was created"
                             #_#_:schema ::user}}}
           (println "Request received for /")
           (ok {:username "ALai57"
                :first_name "Andrew"
                :last_name "Lai"}))

         (GET "/user" req
           :swagger
           {:summary "Test endpoint"
            :produces #{"application/json"}
            :responses {200 {:description "The article that was created"
                             :content {:application/json
                                       {:schema {"$ref" "#/components/schemas/User"}
                                        :examples {:myexample {"$ref" "#/components/examples/myexample"}}}}}}}
           (println "Request received for /user")
           (ok {:username "ALai57"
                :first_name "Andrew"
                :last_name "Lai"}))

         ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
         ;; END: This is my playground, ignore all this
         ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

         (GET "/pet-store/:file-name" [file-name :as req]
           ;; This allows us to serve whatever swagger spec we want from the
           ;; resources folder. If you check out `/resources`, you'll see at least
           ;; two files: `large.json` and `small.json`. Those are both valid
           ;; swagger specs and can be loaded into swagger ui

           (println "REQUEST FOR PET STORE SWAGGER UI" file-name)
           (ok (json/parse-string (slurp (clojure.java.io/resource file-name)))))

         (GET "/swaggerdocs" req
           ;; The `api` function in compojure api generates two different routes
           ;; in your app that handle swagger-related requests. One route is for
           ;; swagger ui and one route is for generating swagger documentation.
           ;; This is the swagger documentation route - lifted from
           ;; compojure.api.swagger.clj.

           (let [runtime-info1 (mw/get-swagger-data req)
                 runtime-info2 (rsm/get-swagger-data req)
                 base-path {:basePath (swag/base-path req)}
                 options (:compojure.api.request/ring-swagger req)
                 paths (:compojure.api.request/paths req)
                 swagger (apply rsc/deep-merge
                                (keep identity [base-path
                                                paths
                                                ;; extra-info is commented
                                                ;; because this is from a
                                                ;; closure around this let
                                                ;; binding and we don't have the
                                                ;; context for it/I don't want
                                                ;; ot recreate it in this tiny
                                                ;; example
                                                #_extra-info
                                                runtime-info1
                                                runtime-info2]))
                 spec (spec-tools.swagger.core/swagger-spec
                       (swagger2/swagger-json swagger options))]

             ;; Prints the results from the let bindings into a file so we can
             ;; inspect
             (with-open [w (clojure.java.io/writer "swagger-docs.log" :append true)]
               (.write w "***********************\n")

               (.write w "*** runtime-info1\n")
               (clojure.pprint/pprint runtime-info1 w)

               (.write w "*** runtime-info2\n")
               (clojure.pprint/pprint runtime-info2 w)

               (.write w "*** base-path\n")
               (clojure.pprint/pprint base-path w)

               (.write w "*** options\n")
               (clojure.pprint/pprint options w)

               (.write w "*** paths\n")
               (clojure.pprint/pprint paths w)

               (.write w "*** swagger\n")
               (clojure.pprint/pprint swagger w)


               (.write w "***********************\n")
               )

             ;; What exactly is this spec thing?
             (println (type spec))

             ;; Some test code we can use to modify the swagger documentation.
             ;; This is just play to see if we can add additional elements to
             ;; swagger docs (i.e. data models or examples)
             (-> spec
                 (assoc :openapi "3.0.2"
                        :components
                        {:schemas
                         {:User {:type :object
                                 :properties {:username {:type :string}
                                              :first_name {:type :string}
                                              :last_name {:type :string}}}}
                         :examples
                         {:myexample {:summary "An example object"
                                      :value {:username "Someexample"
                                              :first_name "Yup"
                                              :last_name "It works"}}}})
                 (dissoc :swagger)
                 ok)))))))

(defn -main
  "Start a reloadable server and start playing around"
  [& args]
  (println "Starting server")
  (httpkit/run-server (wrap-reload #'app) {:port 5000}))
