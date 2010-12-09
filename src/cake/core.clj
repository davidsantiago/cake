(ns cake.core
  (:use cake cake.task
        [cake.file :only [mkdir]]
        [cake.utils.useful :only [update into-map verify merge-in]]
        [clojure.contrib.condition :only [raise]]
        [clojure.string :only [join]])
  (:require [cake.project :as project]))

(defmacro defproject [name version & opts]
  (let [opts (into-map opts)]
    `(do (alter-var-root #'*context*      (fn [_#] {}))
         (alter-var-root #'*project*      (fn [_#] (project/create '~name ~version '~opts)))
         (alter-var-root #'*project-root* (fn [_#] (project/create '~name ~version '~opts))))))

(defmacro defcontext [name & opts]
  (let [opts (into-map opts)]
    `(alter-var-root #'*context* merge-in {'~name '~opts})))

(defmacro undeftask [taskname]
  `(append-task! ~taskname {:replace true}))

(defmacro remove-deps [taskname deps]
  `(append-task! ~taskname {:remove-deps ~deps}))

(defmacro require-tasks [& namespaces]
  `(require-tasks! ~namespaces))

(defmacro deftask
  "Define a cake task. Each part of the body is optional. Task definitions can
   be broken up among multiple deftask calls and even multiple files:
   (deftask foo #{bar baz} ; a set of prerequisites for this task
     \"Documentation for task.\"
     {foo :foo} ; destructuring of *opts*
     (do-something)
     (do-something-else))"
  [name & forms]
  (verify (not (implicit-tasks name)) (str "Cannot redefine implicit task: " name))
  (let [taskname (to-taskname name)
        {:keys [deps docs actions destruct pred]} (parse-task-opts forms)]
    (if (empty? forms)
      `(append-task! ~name {:deps ~deps :docs ~docs})
      `(do
         (defn ~taskname ~(join " " docs) [~destruct]
           (when ~pred ~@actions))
         (append-task! ~name {:actions [~taskname] :deps ~deps :docs ~docs})))))

(defn- in-ts [ts task-decl]
  (conj (drop 2 task-decl)
        (symbol (str ts "." (second task-decl)))
        (first task-decl)))

(defmacro ts
  "Wrap deftask calls with a task namespace. Takes docstrings for the namespace followed by forms.
   Creates a task named after the namespace that prints a list of tasks in that namespace."
  [ts & forms]
  (let [[docs forms] (split-with string? forms)
        docs (update (vec docs) 0 #(str % " --"))]
    `(do
       (deftask ~ts ~@docs
         (invoke ~'help {:help [~(name ts)]}))
       ~@(map (partial in-ts ts) forms))))

(defmacro defile
  "Define a file task. Uses the same syntax as deftask, however the task name
   is a string representing the name of the file to be generated by the body.
   Source files may be specified in the dependencies set, in which case
   the file task will only be ran if the source is newer than the destination.
   (defile \"main.o\" #{\"main.c\"}
     (sh \"cc\" \"-c\" \"-o\" \"main.o\" \"main.c\"))"
  [filename & forms]
  (let [taskname (to-taskname filename)
        {:keys [deps docs actions destruct pred]} (parse-task-opts forms)]
    (if (empty? forms)
      `(append-task! ~filename {:deps ~deps :docs ~docs})
      `(do
         (defn ~taskname ~(join " " docs) [~destruct]
           (when (and ~pred
                      (run-file-task? *File* '~deps))
             (mkdir (.getParentFile *File*))
             ~@actions))
         (append-task! ~filename {:actions [~taskname] :deps ~deps :docs ~docs})))))

(defmacro invoke [name & [opts]]
  `(binding [*opts* (or ~opts *opts*)]
     (run-task '~name)))

(defmacro bake [& args]
  `(project/bake ~@args))

(defn abort-task [& message]
  (raise {:type :abort-task :message (join " " message)}))