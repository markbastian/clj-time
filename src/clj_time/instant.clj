(ns clj-time.instant
  "An optional convenience namespaces that allows key JodaTime types
    to be transparently serialized with the Clojure reader (via instant literals)."
  (:import (java.time ZonedDateTime LocalDate ZoneId)
           (java.util Date)))

(defmethod print-dup ZonedDateTime
  [^ZonedDateTime d out]
  (print-dup (Date/from (.toInstant d)) out))

(defmethod print-dup LocalDate
  [^LocalDate d out]
  (print-dup (Date/from (.atStartOfDay d (ZoneId/systemDefault))) out))
