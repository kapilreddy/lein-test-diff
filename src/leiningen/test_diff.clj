(ns leiningen.test-diff
  (:require [clojure.tools.namespace.track :as track]
            [clojure.tools.namespace.dependency :as dep]
            [clojure.tools.namespace.find :as find]
            [clojure.tools.namespace.file :as file]
            [clojure.set :as cs]
            [clojure.string :as cstr]))


(defn recursive-transitive-dependents*
  [g nodes acc valid-path?]
  (if-let [node (first nodes)]
    (if (valid-path? node)
      (if-let [n-xs (seq (dep/immediate-dependents g node))]
        (recur g
               (reduce conj
                       (rest nodes)
                       n-xs)
               (reduce conj
                       acc
                       (conj n-xs node))
               valid-path?)
        (recur g
               (rest nodes)
               (conj acc node)
               valid-path?))
      (recur g
             (rest nodes)
             acc
             valid-path?))
    acc))


(defn recursive-transitive-dependents
  [g node valid-path?]
  (recursive-transitive-dependents* g
                                    #{node}
                                    #{}
                                    valid-path?))



(defn test-diff*
  [source-paths test-paths files & {:keys [exclude-paths timeout-secs]
                                    :or {exclude-paths []
                                         timeout-secs 60}}]
  (when (seq files)
    (future (Thread/sleep (* timeout-secs 1000))
            (System/exit 1))
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
          src-graph-set (set (dep/nodes src-graph))


          diff-ns-set (set (mapcat #(find/find-namespaces-in-dir (java.io.File. %))
                                   files))

          exclude-ns-set (set (mapcat #(find/find-namespaces-in-dir (java.io.File. %))
                                      exclude-paths))
          valid-path? (fn [n]
                        (or (and (not (exclude-ns-set n))
                                 (src-graph-set n))
                            (diff-ns-set n)))]

      (cs/difference (set (mapcat (fn [diff-ns]
                                    (recursive-transitive-dependents graph
                                                                     diff-ns
                                                                     valid-path?))
                                  diff-ns-set))
                     src-graph-set))))


(defn test-diff
  [project]
  (let [s (slurp *in*)
        files (cstr/split s
                          #"\n")
        diff-ns-xs (test-diff* (:source-paths project)
                               (:test-paths project)
                               files
                               :timeout-secs
                               (if-let [env-timeout (get (System/getenv)
                                                         "LEIN_TEST_DIFF_TIMEOUT")]
                                 (Integer/parseInt env-timeout)
                                 (get-in project
                                         [:test-diff :timeout]
                                         60))

                               :exclude-paths (get-in project
                                                      [:test-diff :exclude-paths]
                                                      []))]
    (println (cstr/join "\n"
                        diff-ns-xs))))
