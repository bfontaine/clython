# Clython

**Clython** is Clojure wrapper around [Jython][], aka Python on the JVM.

[Jython]: https://www.jython.org/

## Usage

```clojure
(ns my.ns
  (:require [clython.core :as cly]))

(def re-findall (cly/python-import "re" "findall"))

(re-findall "\\w+" "hello from clojure")
; => ("hello" "from" "clojure")

;; equivalent of `$ python -m SimpleHTTPServer 8000`
(def http-server (cly/python-import "SimpleHTTPServer" "test"))
(http-server)
; => Serving HTTP on 0.0.0.0 port 8000 ...

(cly/python-import "sys" "version")
; => "2.7.1 (default:0df7adb1b397, Jun 30 2017, 19:02:43) \n[Java HotSpot(TM) 64-Bit Server VM (Oracle Corporation)]"
```

## License

Copyright © 2019 Baptiste Fontaine

This program and the accompanying materials are made available under the terms
of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published
by the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
