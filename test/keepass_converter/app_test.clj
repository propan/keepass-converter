(ns keepass-converter.app-test
  (:require [clojure.test :refer :all]
            [clojure.java.io :as io]
            [net.cgrand.enlive-html :as enlive]
            [keepass-converter.app :refer :all]))

(def entries
  [{:title    "Gmail"
    :url      "http://gmail.com"
    :username "user"
    :password "password"
    :comment  "multiline\nsecret\ncomment"}
   {:title    "PayPal"
    :url      "https://paypal.com"
    :username "user@gmail.com"
    :password "another-password"}])

(deftest get-output-file-name-test
  (testing "Input file has dots in the name"
    (let [result (get-output-file-name (io/file "export.12-12-12.xml") {})]
      (is (= result "export.12-12-12.csv"))))
  
  (testing "No output argument is given"
    (let [result (get-output-file-name (io/file "test.xml") {})]
      (is (= result "test.csv"))))

  (testing "Output argument is given"
    (let [result (get-output-file-name (io/file "test.xml") {:output "/tmp/test.csv"})]
      (is (= result "/tmp/test.csv")))))

(deftest extract-entries-test
  (let [result (-> (io/file "resources/export.xml")
                   (enlive/xml-resource)
                   (enlive/select [:entry])
                   (extract-entries)
                   (convert-multiline-comments)
                   (convert-entries-to-csv))]
    (is (= result [["PayPal" "http://paypal.com" "my-secret-name@my-domain.com" "my-secret-password" "tiny secret note"]
                   ["Gmail" "http://gmail.com" "my-secret-name@gmail.com" "another-secret-password" "Q: Do you know the answer on my secret question? A: I hope I do"]]))))

(deftest convert-entries-to-csv-test
  (let [result (convert-entries-to-csv entries)]
    (is (= result [["Gmail" "http://gmail.com" "user" "password" "multiline\nsecret\ncomment"]
                   ["PayPal" "https://paypal.com" "user@gmail.com" "another-password" ""]]))))

(deftest convert-multiline-comments-test
  (let [result (convert-multiline-comments entries)]
    (is (= result [{:comment "multiline secret comment"
                    :title "Gmail"
                    :username "user"
                    :url "http://gmail.com"
                    :password "password"}
                   {:title "PayPal"
                    :username "user@gmail.com"
                    :url "https://paypal.com"
                    :password "another-password"}]))))
