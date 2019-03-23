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
       {}
       {"foo" "bar"}
       {"hey" nil}
       {1 2, 3 4}
       {1 2, "mixed" "types", "" false}
       {"a" ["b" ["c"]]}
       {"a" [{"b" [{"c" 1, "d" 2} true] 1 nil} nil]}

       []
       [42]
       [1 [2 [3 [4 5] 6 [7]] [8 9] [10]] 11]

       ))

(deftest python-import
  (testing "Function coercion"
    (is (ifn? (cly/python-import "re" "findall"))))
  (testing "String coercion"
    (is (string? (cly/python-import "sys" "version")))))
