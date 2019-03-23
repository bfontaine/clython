(ns clython.core
  (:require [clojure.string :as str])
  (:import [org.python.core
                            Py
                            PyArray
                            PyBoolean
                            PyBytecode
                            PyDictionary
                            PyFloat
                            PyFrame
                            PyFunction
                            PyLong
                            PyNone
                            PyObject
                            PyString
                            PyUnicode

                            imp
                            ]
           [clojure.lang
                         IFn
                         Keyword
                         PersistentArrayMap
                         PersistentHashMap
                         PersistentVector
                         ]
           [java.util HashMap]))

;;
;; Internal Utilities
;; ==================

(def ^:private empty-objects (into-array PyObject nil))
(def ^:private empty-strings (into-array String nil))

(def ^:private
  base-code
  (PyBytecode. 0 0 0 0 "" empty-objects empty-strings empty-strings "" "" 0 ""))

(def ^:private
  base-frame
  (PyFrame. base-code (PyDictionary.)))

(defn- jy?
  [x]
  (instance? PyObject x))

;;
;; Conversions
;; ===========
;;
;; Clojure -> Jython
;; -----------------

(def ^:private jy-true Py/True)
(def ^:private jy-false Py/False)
(def ^:private jy-none Py/None)

(defmulti ^:private clj->jy* type)

(defn clj->jy
  [x]
  (if (jy? x)
    x
    (clj->jy* x)))

(defmethod clj->jy* nil [_] jy-none)

(defmethod clj->jy* Boolean [x] (if x jy-true jy-false))

(defmethod clj->jy* String [s] (PyUnicode. ^String s))
(defmethod clj->jy* Integer [i] (PyLong. ^Long (long i)))
(defmethod clj->jy* Long [l] (PyLong. ^Long l))
(defmethod clj->jy* Float [f] (PyFloat. ^Float f))
(defmethod clj->jy* Double [d] (PyFloat. ^Double d))
(defmethod clj->jy* Character [c] (PyUnicode. (str c)))
(defmethod clj->jy* Keyword [k] (PyUnicode. (name k)))

(defn- map->jy
  [m]
  (let [hm (HashMap. (count m))]
    (doseq [[k v] m]
      (let [jy-k (clj->jy k)
            jy-v (clj->jy v)]
        (.put hm jy-k jy-v)))
    (PyDictionary. hm)))

(defmethod clj->jy* PersistentHashMap [m] (map->jy m))
(defmethod clj->jy* PersistentArrayMap [m] (map->jy m))

(defmethod clj->jy* PersistentVector
  [v]
  (PyArray. PyObject (into-array PyObject (map clj->jy v))))

;;
;; Jython -> Clojure
;; -----------------

(defmulti jy->clj type)

(defmethod jy->clj PyNone [_] nil)
(defmethod jy->clj PyUnicode [s] (.asString s))
(defmethod jy->clj PyString [s] (.asString s))
(defmethod jy->clj PyBoolean [b] (= jy-true b))
(defmethod jy->clj PyLong [l] (.asLong l))
(defmethod jy->clj PyFloat [f] (.asDouble f))
(defmethod jy->clj PyDictionary [d] (into {} (map (fn [[k v]] [(jy->clj k)
                                                               (jy->clj v)]) d)))
(defmethod jy->clj PyArray [a] (mapv jy->clj (.asIterable a)))

(defmethod jy->clj PyFunction
  [f]
  (reify IFn
    (invoke [_]
      (.__call__ f))
    (invoke [_ a]
      (.__call__ f (clj->jy a)))
    (invoke [_ a b]
      (.__call__ f (clj->jy a) (clj->jy b)))
    (invoke [_ a b c]
      (.__call__ f (clj->jy a) (clj->jy b) (clj->jy c)))
    (invoke [_ a b c d]
      (.__call__ f (clj->jy a) (clj->jy b) (clj->jy c) (clj->jy d)))
    (invoke [_ a b c d e & rest*]
      (.__call__ f (into-array PyObject (concat [a b c d e] rest*))))))

(def ^:private
  pyobject-array-class
  (Class/forName "[Lorg.python.core.PyObject;"))

(defmethod jy->clj :default [x]
  (cond
    (instance? pyobject-array-class x)
    (mapv jy->clj x)

    :else x))

;;
;; Modules Import
;; ==============

(defn python-import
  ([module]
   (. imp importOne module base-frame))
  ([module names]
   (let [one? (string? names)
         names (if one? [names] names)]
     (cond->> (. imp importFrom module (into-array String names) base-frame 0)
       true (map jy->clj)
       one? first))))

(defn- sym->jy-name
  [name*]
  (-> name* name (str/replace #"-" "_")))

(defn- split-names-aliases
  [names]
  (if (map? names)
    [(mapv sym->jy-name (keys names)) (vec (vals names))]
    [(mapv sym->jy-name names) names]))

(defmacro let-import
  "(let-import [pygments [highlight]
                pygments.lexer [get-lexer-by-name guess-lexer]
                pygments.formatters [get-formatter-by-name]
                pygments.util {ClassNotFound class-not-found}]
     ...)"
  [bindings & body]
  {:pre [(vector? bindings)
         (zero? (mod (count bindings) 2))]}
  (let [let-bindings (vec
                       (mapcat (fn [[python-module names]]
                                 (let [module-name (name python-module)
                                       [jy-names
                                        aliases]   (split-names-aliases names)]
                                   `[~aliases (python-import ~module-name (into-array String ~jy-names))]))
                               (partition 2 bindings)))]
    `(let ~let-bindings
       ~@body)))
