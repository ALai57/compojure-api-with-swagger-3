(ns compojure-api-with-swagger-3.y
  (:gen-class)
  (:require [clojure.spec.alpha :as s]

            [org.httpkit.server :as httpkit]

            [cheshire.core :as json]

            [compojure.api.sweet :refer [api context GET POST undocumented]]
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
(def app
  (api
   (context "/api/v1" []
     (POST "/user" req
       :swagger
       {:summary "Test endpoint"
        :produces #{"application/json"}
        :requestBody
        {:content {:application/json
                   {:schema {"$ref" "#/components/schemas/User"}
                    :examples {:myexample {"$ref" "#/components/examples/myexample"}
                               :myexample2 {"$ref" "#/components/examples/myexample2"}}}}}
        :responses
        {200 {:description "The article that was created"
              :content {:application/json
                        {:schema {"$ref" "#/components/schemas/User"}
                         :examples {:myexample {"$ref" "#/components/examples/myexample"}}}}}}}
       (println "POST Request received for /user")
       (ok {:username "ALai57"
            :first_name "Andrew"
            :last_name "Lai"})))

   (undocumented
    (swagger-ui/swagger-ui {:path "/swagger"
                            :swagger-docs "/swagger.json"}))

   (GET "/swagger.json" req
     (let [runtime-info1 (mw/get-swagger-data req)
           runtime-info2 (rsm/get-swagger-data req)
           base-path {:basePath (swag/base-path req)}
           options (:compojure.api.request/ring-swagger req)
           paths (:compojure.api.request/paths req)
           swagger (apply rsc/deep-merge
                          (keep identity [base-path
                                          paths
                                          runtime-info1
                                          runtime-info2]))
           spec (spec-tools.swagger.core/swagger-spec
                 (swagger2/swagger-json swagger options))]

       (-> spec
           (assoc :openapi "3.0.2"
                  :info {:title "Testing Swagger 3.x"
                         :description "A playground for experimenting"}
                  :tags [{:name "api", :description "My awesome API"}]
                  :components
                  {:schemas
                   {:User {:type :object
                           :properties {:username {:type :string}
                                        :first_name {:type :string}
                                        :last_name {:type :string}}}
                    :Pet {:type :object
                          :properties {:name {:type :string}
                                       :type {:type :string
                                              :enum [:dog :cat]}
                                       :has_tail {:type :boolean}}}}
                   :examples
                   {:myexample {:summary "An example object"
                                :value {:username "An example"
                                        :first_name "Yup"
                                        :last_name "It works"}}
                    :myexample2 {:summary "A different example"
                                 :value {:username "Other example"
                                         :first_name "Yay!"
                                         :last_name "This is different"}}}})
           (dissoc :swagger)
           ok)))))
