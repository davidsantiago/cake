(ns cake.tasks.ng
  (:use cake cake.core [cake.utils.useful :only [if-ns]]))

(if-ns (:import vimclojure.nailgun.NGServer)
  (do
    (defn start-ng-server  [host]
      (bake [host host]
            (vimclojure.nailgun.NGServer/main  (into-array [host]))))

    (deftask ng #{}
      "Start the nailgun server."
      (start-ng-server "127.0.0.1"))))
