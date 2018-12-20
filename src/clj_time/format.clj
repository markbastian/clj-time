(ns clj-time.format
  "Utilities for parsing and unparsing DateTimes as Strings.

   Parsing and printing are controlled by formatters. You can either use one
   of the built in ISO 8601 and a single RFC 822 formatters or define your own, e.g.:

     (def built-in-formatter (formatters :basic-date-time))
     (def custom-formatter (formatter \"yyyyMMdd\"))

   To see a list of available built-in formatters and an example of a date-time
   printed in their format:

    (show-formatters)

   Once you have a formatter, parsing and printing are straightforward:

     => (parse custom-formatter \"20100311\")
     #<DateTime 2010-03-11T00:00:00.000Z>

     => (unparse custom-formatter (date-time 2010 10 3))
     \"20101003\"

   By default the parse function always returns a DateTime instance with a UTC
   time zone, and the unparse function always represents a given DateTime
   instance in UTC. A formatter can be modified to different timezones, locales,
   etc with the functions with-zone, with-locale, with-chronology,
   with-default-year and with-pivot-year."
  (:refer-clojure :exclude [extend second])
  (:require [clj-time.core :refer :all]
            [clojure.set :refer [difference]])
  (:import [java.util Locale]
           [java.time.format DateTimeFormatter]
           [java.time.chrono Chronology]
           (java.time LocalTime LocalDate ZonedDateTime LocalDateTime Period)))

(declare formatters)
;; The formatters map and show-formatters idea are strait from chrono.

(defn ^DateTimeFormatter formatter
  "Returns a custom formatter for the given date-time pattern or keyword."
  ([fmts]
   (formatter fmts utc))
  ([fmts dtz]
   (cond (keyword? fmts) (.withZone ^DateTimeFormatter (get formatters fmts) dtz)
         (string? fmts) (.withZone (DateTimeFormatter/ofPattern fmts) dtz)
         :else (.withZone ^DateTimeFormatter fmts dtz))))

(defn formatter-local
  "Returns a custom formatter with no time zone info."
  ([^String fmt]
   (DateTimeFormatter/ofPattern fmt)))

(defn with-chronology
  "Return a copy of a formatter that uses the given Chronology."
  [^DateTimeFormatter f ^Chronology c]
  (.withChronology f c))

(defn with-locale
  "Return a copy of a formatter that uses the given Locale."
  [^DateTimeFormatter f ^Locale l]
  (.withLocale f l))

;Java time DateTimeFormatter doesn't have a pivot or default year :(
;Consider using appendValueReduced to work around (if it does work).
;https://stackoverflow.com/questions/29490893/parsing-string-to-local-date-doesnt-use-desired-century
;https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatterBuilder.html#appendValueReduced-java.time.temporal.TemporalField-int-int-int-

;(defn with-pivot-year
;  "Return a copy of a formatter that uses the given pivot year."
;  [^DateTimeFormatter f ^Long pivot-year]
;  (.withPivotYear f pivot-year))

(defn with-zone
  "Return a copy of a formatter that uses the given DateTimeZone."
  [^DateTimeFormatter f dtz]
  (.withZone f dtz))

;(defn with-default-year
;  "Return a copy of a formatter that uses the given default year."
;  [^DateTimeFormatter f ^Integer default-year]
;  (.withDefaultYear f default-year))


(def ^{:doc "Map of ISO 8601 and a single RFC 822 formatters that can be used for parsing and, in most
             cases, printing."}
formatters
  (into {} (map
             (fn [[k ^DateTimeFormatter f]] [k (.withZone f utc)])
             {:basic-date       DateTimeFormatter/BASIC_ISO_DATE
              :date             DateTimeFormatter/ISO_DATE
              :basic-date-time  DateTimeFormatter/ISO_DATE_TIME
              :instant          DateTimeFormatter/ISO_INSTANT
              :local-date       DateTimeFormatter/ISO_LOCAL_DATE
              :loca-date-time   DateTimeFormatter/ISO_LOCAL_DATE_TIME
              :local-time       DateTimeFormatter/ISO_LOCAL_TIME
              :offset-date      DateTimeFormatter/ISO_OFFSET_DATE
              :offset-date-time DateTimeFormatter/ISO_OFFSET_DATE_TIME
              :offset-time      DateTimeFormatter/ISO_OFFSET_TIME
              :ordinal-date     DateTimeFormatter/ISO_ORDINAL_DATE
              :time             DateTimeFormatter/ISO_TIME
              :week-date        DateTimeFormatter/ISO_WEEK_DATE
              :zoned-date-time  DateTimeFormatter/ISO_ZONED_DATE_TIME
              :rfc1123          DateTimeFormatter/RFC_1123_DATE_TIME
              :rfc822           (with-locale (formatter "EEE, dd MMM yyyy HH:mm:ss Z") Locale/US)
              :mysql            (formatter "yyyy-MM-dd HH:mm:ss")})))

(def ^{:private true} parsers
  #{:date-element-parser :date-opt-time :date-parser :date-time-parser
    :local-date-opt-time :local-date :local-time :time-element-parser
    :time-parser})

(def ^{:private true} printers
  (difference (set (keys formatters)) parsers))

(defn ^ZonedDateTime parse
  "Returns a DateTime instance in the UTC time zone obtained by parsing the
   given string according to the given formatter."
  ([^DateTimeFormatter fmt ^String s]
   (.parse fmt s))
  ([^String s]
   (first
     (for [f (vals formatters)
           :let [d (try (parse f s) (catch Exception _ nil))]
           :when d] d))))

(defn ^LocalDateTime parse-local
  "Returns a LocalDateTime instance obtained by parsing the
   given string according to the given formatter."
  ([^DateTimeFormatter fmt ^String s]
   (.parse fmt s))
  ([^String s]
   (first
     (for [f (vals formatters)
           :let [d (try (parse-local f s) (catch Exception _ nil))]
           :when d] d))))

(defn ^LocalDate parse-local-date
  "Returns a LocalDate instance obtained by parsing the
   given string according to the given formatter."
  ([^DateTimeFormatter fmt ^String s]
   (.parse fmt s))
  ([^String s]
   (first
     (for [f (vals formatters)
           :let [d (try (parse-local-date f s) (catch Exception _ nil))]
           :when d] d))))

(defn ^LocalTime parse-local-time
  "Returns a LocalTime instance obtained by parsing the
  given string according to the given formatter."
  ([^DateTimeFormatter fmt ^String s]
   (.parse fmt s))
  ([^String s]
   (first
     (for [f (vals formatters)
           :let [d (try (parse-local-time f s) (catch Exception _ nil))]
           :when d] d))))

(defn unparse
  "Returns a string representing the given DateTime instance in UTC and in the
  form determined by the given formatter."
  [^DateTimeFormatter fmt dt]
  (.format fmt dt))

(defn unparse-local
  "Returns a string representing the given LocalDateTime instance in the
  form determined by the given formatter."
  [^DateTimeFormatter fmt dt]
  (.format fmt dt))

(defn unparse-local-date
  "Returns a string representing the given LocalDate instance in the form
  determined by the given formatter."
  [^DateTimeFormatter fmt ld]
  (.format fmt ld))

(defn unparse-local-time
  "Returns a string representing the given LocalTime instance in the form
  determined by the given formatter."
  [^DateTimeFormatter fmt lt]
  (.format fmt lt))


(defn show-formatters
  "Shows how a given DateTime, or by default the current time, would be
  formatted with each of the available printing formatters."
  ([] (show-formatters (now)))
  ([dt]
   (doseq [p (sort printers)]
     (let [fmt (formatters p)]
       (printf "%-40s%s\n" p (unparse fmt dt))))))

(defprotocol Mappable
  (instant->map [instant] "Returns a map representation of the given instant.
                          It will contain the following keys: :years, :months,
                          :days, :hours, :minutes and :seconds."))

(defn- to-map [years months days hours minutes seconds]
  {:years   years
   :months  months
   :days    days
   :hours   hours
   :minutes minutes
   :seconds seconds})

(extend-protocol Mappable
  ZonedDateTime
  (instant->map [dt]
    (to-map
      (year dt)
      (month dt)
      (day dt)
      (hour dt)
      (minute dt)
      (second dt))))

(extend-protocol Mappable
  Period
  (instant->map [period]
    (to-map
      (year period)
      (month period)
      (day period)
      (hour period)
      (minute period)
      (second period))))

;TODO - MSB - where is this used?
;(extend-protocol Mappable
;  Interval
;  (instant->map [it]
;    (instant->map (.toPeriod it (PeriodType/yearMonthDayTime)))))
