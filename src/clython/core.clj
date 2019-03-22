(ns clython.core
  (:require [clojure.string :as str])
  (:import ;[org.python.util PythonInterpreter]
           [org.python.core PyObject PyNone PyBoolean PyLong PyFloat
                            PyDictionary PyUnicode

                            imp PyBytecode PyFrame
                            ]
         ; [clojure.lang Keyword PersistentArrayMap PersistentHashMap]
           ))

(def ^:private empty-objects (into-array PyObject nil))
(def ^:private empty-strings (into-array String nil))

(def ^:private
  base-code
  (PyBytecode. 0 0 0 0 "" empty-objects empty-strings empty-strings "" "" 0 ""))

(def ^:private
  base-frame
  (PyFrame. base-code (PyDictionary.)))

(defn python-import
  ([module]
   (. imp importOne module base-frame))
  ([module names]
   (let [one (string? names)
         names (if one [names] names)]
     (cond-> (. imp importFrom module (into-array String names) base-frame 0)
       one first))))

(defn- clj->py-name
  [name*]
  (-> name* str (str/replace #"-" "_")))

(defn- split-names-aliases
  [names]
  (if (map? names)
    [(mapv clj->py-name (keys names)) (vec (vals names))]
    [(mapv clj->py-name names) names]))

(defmacro let-import
  "(let-import [pygments [highlight]
                pygments.lexer [get-lexer-by-name guess-lexer]
                pygments.formatter [get-formatter-by-name]
                pygments.util {ClassNotFound class-not-found}]
     ...)"
  [bindings & body]
  {:pre [(vector? bindings)
         (zero? (mod (count bindings) 2))]}
  (let [let-bindings (vec
                       (mapcat (fn [[python-module names]]
                                 (let [module-name (str python-module)
                                       [py-names
                                        aliases]   (split-names-aliases names)]
                                   `[~aliases (python-import ~module-name (into-array String ~py-names))]))
                               (partition 2 bindings)))]
    `(let ~let-bindings
       ~@body)))
