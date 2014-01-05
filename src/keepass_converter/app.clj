(ns keepass-converter.app
  (:require [clojure.tools.cli :refer [parse-opts]]
            [clojure.string :as string]
            [clojure.java.io :as io]
            [clojure.data.csv :as csv]
            [net.cgrand.enlive-html :as enlive])
  (:gen-class :main true))

(def entry-fields
  [:title :url :username :password :comment])

(def cli-options
  [["-o" "--output=1password.csv" "Output file in CSV format"]
   ["-i" "--interactive" "You will be prompt about conversion of every entry in the input file"
    :default false]
   ["-h" "--help"]])

(defn usage
  [options-summary]
  (->> ["A command-line utility to convert KeePass export files to CSV format."
        ""
        "Usage: keepass-converter --output=output.csv input.xml"
        ""
        "input.xml - an input XML file in KeePass format"
        ""
        "Options:"
        options-summary
        ""]
       (string/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn get-output-file-name
  [input-file {:keys [output]}]
  (if-not output
    (let [file-name (.getName input-file)
          dot-pos   (.lastIndexOf file-name 46)]
      (if (pos? dot-pos)
        (str (.substring file-name 0 dot-pos) ".csv")
        (str file-name ".csv")))
    output))

(defn prompt
  "Prompts a user with the given question and a set of acceptable answers."
  [question options]
  (loop [a nil]
    (if (contains? options a)
      a
      (do
        (println question)
        (recur (read-line))))))

(defn extract-field
  [entry res field]
  (let [value (enlive/select entry [field :> enlive/text-node])]
    (if-not (empty? value)
      (assoc res field (apply str value))
      res)))

(defn extract-entry
  [entry]
  (reduce (partial extract-field entry) {} entry-fields))

(defn extract-entries
  [entries]
  (map extract-entry entries))

(defn convert-multiline-comments*
  [{:keys [comment] :as entry}]
  (if comment
    (assoc entry :comment (clojure.string/replace comment #"\r?\n" " "))
    entry))

(defn convert-multiline-comments
  "Removes s on \\n or \\r\\n from comments."
  [entries]
  (map convert-multiline-comments* entries))

(defn convert-entry-to-seq
  [entry seq field]
  (let [v (field entry)]
    (if (nil? v)
      (conj seq "")
      (conj seq v))))

(defn convert-entries-to-csv
  [entries]
  (map #(reduce (partial convert-entry-to-seq %) [] entry-fields) entries))

(defn format-export-prompt
  [[title _ username]]
  (->> [""
        (str "Title: " title)
        (str "Username: " username)
        ""
        "Include? [y/n]"
        ""]
       (string/join \newline)))

(defn export-to-csv
  [entries output-file interactive?]
  (with-open [out (io/writer output-file :append false)]
    (if interactive?
      (doseq [entry entries]
        (when (= "y" (prompt (format-export-prompt entry) #{"y" "n"}))
          (csv/write-csv out [entry] :quote? (constantly true))))
      (csv/write-csv out entries :quote? (constantly true)))))

(defn -main
  [& args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
     (:help options)            (exit 0 (usage summary))
     (not= (count arguments) 1) (exit 1 (usage summary))
     errors                     (exit 1 (error-msg errors)))
    
    (let [input-file  (io/file (first arguments))
          output-file (io/file (get-output-file-name input-file options))]
      ;; check if the input file exists and is readable
      (when (or (not (.exists input-file))
                (not (.canRead input-file)))
        (exit 1 (error-msg [(str input-file ": does not exist or cannot be read")])))
      ;; check if the output file already exists
      (if (.exists output-file)
        (when (= "n" (prompt (str output-file " already exists. Do you want to overwrite it? [y/n]") #{"y" "n"}))
          (exit 0 "Aborting at user request."))
        (.createNewFile output-file))
      ;; check if the output file is accessible for writing
      (when (not (.canWrite output-file))
        (exit 1 (error-msg [(str output-file ": cannot be open for writing")])))

      (println (str "Converting: " input-file " -> " output-file))

      (-> (enlive/xml-resource input-file)
          (enlive/select [:entry])
          (extract-entries)
          (convert-multiline-comments)
          (convert-entries-to-csv)
          (export-to-csv output-file (:interactive options))))))
