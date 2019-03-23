(ns clython.core-test
  (:require [clojure.test :refer :all]
            [clython.core :as cly]))

(deftest clj->jy->clj
  (are [x] (= x (cly/jy->clj (cly/clj->jy x)))
       0 1 2 3
       -1 -2 -42
       1000
       true
       false
       nil
       "" "hey" "Ô¢¬Æ⁄Ó|·"
       {"foo" "bar"}
       {1 2, 3 4}
       {1 2, "mixed" "types", "" false}))

(deftest python-import
  (testing "Function coercion"
    (is (ifn? (cly/python-import "re" "findall"))))
  (testing "String coercion"
    (is (string? (cly/python-import "sys" "version")))))
