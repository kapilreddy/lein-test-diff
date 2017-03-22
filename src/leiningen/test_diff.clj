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
            src-files (mapv #(java.io.File. %)
                            source-paths)
            src-graph (::track/deps
                       (file/add-files (dep/graph)
                                       (mapcat (fn [f]
                                                 (find/find-clojure-sources-in-dir f))
                                               src-files)))
            diff-ns-xs (mapcat #(find/find-namespaces-in-dir (java.io.File. %))
                               files)
            test-diff-ns (cs/difference (set (mapcat (fn [diff-ns]
                                                       (dep/transitive-dependents graph
                                                                                  diff-ns))
                                                     diff-ns-xs))
                                        (set (dep/nodes src-graph)))]

        (println (cstr/join "\n"
                            test-diff-ns)))))


(defn test-diff
  [project]
  (let [s (slurp *in*)
        files (cstr/split s
                          #"\n")]
    (test-diff* (:source-paths project)
                (:test-paths project)
                files)))
