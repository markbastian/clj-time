(ns clj-time.jdbc
  "clojure.java.jdbc protocol extensions supporting DateTime coercion.

  To use in your project, just require the namespace:

    => (require 'clj-time.jdbc)
    nil

  Doing so will extend the protocols defined by clojure.java.jdbc, which will
  cause java.sql.Timestamp objects in JDBC result sets to be coerced to
  org.joda.time.DateTime objects, and vice versa where java.sql.Timestamp
  objects would be required by JDBC."
  (:require [clj-time.coerce :as tc]
            [clojure.java.jdbc :as jdbc])
  (:import (java.sql Timestamp Date Time)
           (java.time ZonedDateTime LocalTime)))

; http://clojure.github.io/java.jdbc/#clojure.java.jdbc/IResultSetReadColumn
(extend-protocol jdbc/IResultSetReadColumn
  Timestamp
  (result-set-read-column [v _2 _3]
    (tc/from-sql-time v))
  Date
  (result-set-read-column [v _2 _3]
    (tc/from-sql-date v))
  Time
  (result-set-read-column [v _2 _3]
    ;TODO - MSB - This is almoset certainly wrong
    (LocalTime v)))

; http://clojure.github.io/java.jdbc/#clojure.java.jdbc/ISQLValue
(extend-protocol jdbc/ISQLValue
  ;TODO - MSB - This is almoset certainly wrong
  ZonedDateTime
  (sql-value [v]
    (tc/to-sql-time v)))
