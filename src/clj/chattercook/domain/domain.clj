(ns chattercook.domain.domain
  (:require
    [clojure.string :as string]))

(defn possessive [name]
  (if (some (fn [c] (string/ends-with? name c)) ["x" "s" "z" "ß" "ce"])
    (str name "'")
    (str name "s")))
