(ns leiningen.test-diff
  (:require [clojure.tools.namespace.track :as track]
            [clojure.tools.namespace.dependency :as dep]
            [clojure.tools.namespace.find :as find]
            [clojure.tools.namespace.file :as file]
            [clojure.set :as cs]
            [clojure.string :as cstr]))

(defn test-diff*
  [source-paths test-paths files]
  (when (seq files)
      (let [graph (::track/deps
                   (file/add-files (dep/graph)
                                   (mapcat (fn [f]
                                             (find/find-clojure-sources-in-dir f))
                                           (map #(java.io.File. %)
                                                (concat source-paths
                                                        test-paths)))))
            test-files (mapv #(java.io.File. %)
                             test-paths)
            test-graph (::track/deps
                        (file/add-files (dep/graph)
                                        (mapcat (fn [f]
                                                  (find/find-clojure-sources-in-dir f))
                                                test-files)))
            diff-ns-xs (mapcat #(find/find-namespaces-in-dir (java.io.File. %))
                               files)]

        (println (cstr/join "\n"
                            (cs/intersection (set (dep/nodes test-graph))
                                             (set (mapcat (fn [diff-ns]
                                                            (dep/transitive-dependents graph
                                                                                       diff-ns))
                                                          diff-ns-xs))))))))


(defn test-diff
  [project]
  (let [s (slurp *in*)
        files (cstr/split s
                          #"\n")]
    (test-diff* (:source-paths project)
                (:test-paths project)
                files)))
