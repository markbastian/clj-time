(ns clj-time.types
  "This namespace defines a set of predicates for the various Joda Time types used by clj-time."
  (:import [java.time ZonedDateTime LocalDate LocalDateTime ZoneId]))

(defn date-time? [x]
  (instance? ZonedDateTime x))

(defn local-date-time? [x]
  (instance? LocalDateTime x))

(defn local-date? [x]
  (instance? LocalDate x))

(defn time-zone? [x]
  (instance? ZoneId x))
