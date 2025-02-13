(ns chattercook.domain.domain-test
  (:require
    [chattercook.domain.domain :refer [possessive]]
    [clojure.test :refer :all]))


(deftest event-name-test
  (is (= "Max’" (possessive "Max")))
  (is (= "Jens’" (possessive "Jens")))
  (is (= "Trotz’" (possessive "Trotz")))
  (is (= "Beatrice’" (possessive "Beatrice")))
  (is (= "Scheiß’" (possessive "Scheiß")))
  (is (= "Stefans" (possessive "Stefan"))))
