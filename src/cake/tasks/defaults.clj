(ns cake.tasks.defaults
  (:use cake)
  (:require cake.tasks.jar
            cake.tasks.test
            cake.tasks.compile
            cake.tasks.dependencies
            cake.tasks.swank))

(deftask help
  (println "this is the help command"))

