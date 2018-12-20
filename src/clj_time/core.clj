(ns clj-time.core
  "The core namespace for date-time operations in the clj-time library.

   Create a DateTime instance with date-time (or a LocalDateTime instance with local-date-time),
   specifying the year, month, day, hour, minute, second, and millisecond:

     => (date-time 1986 10 14 4 3 27 456)
     #<DateTime 1986-10-14T04:03:27.456Z>

     => (local-date-time 1986 10 14 4 3 27 456)
     #<LocalDateTime 1986-10-14T04:03:27.456>

   Less-significant fields can be omitted:

     => (date-time 1986 10 14)
     #<DateTime 1986-10-14T00:00:00.000Z>

     => (local-date-time 1986 10 14)
     #<LocalDateTime 1986-10-14T00:00:00.000>

   Get the current time with (now) and the start of the Unix epoch with (epoch).

   Once you have a date-time, use accessors like hour and second to access the
   corresponding fields:

     => (hour (date-time 1986 10 14 22))
     22

     => (hour (local-date-time 1986 10 14 22))
     22

   The date-time constructor always returns times in the UTC time zone. If you
   want a time with the specified fields in a different time zone, use
   from-time-zone:

     => (from-time-zone (date-time 1986 10 22) (time-zone-for-offset -2))
     #<DateTime 1986-10-22T00:00:00.000-02:00>

   If on the other hand you want a given absolute instant in time in a
   different time zone, use to-time-zone:

     => (to-time-zone (date-time 1986 10 22) (time-zone-for-offset -2))
     #<DateTime 1986-10-21T22:00:00.000-02:00>

   In addition to time-zone-for-offset, you can use the time-zone-for-id and
   default-time-zone functions and the utc Var to construct or get DateTimeZone
   instances.

   The functions after? and before? determine the relative position of two
   DateTime instances:

     => (after? (date-time 1986 10) (date-time 1986 9))
     true

     => (after? (local-date-time 1986 10) (local-date-time 1986 9))
     true

   Often you will want to find a date some amount of time from a given date. For
   example, to find the time 1 month and 3 weeks from a given date-time:

     => (plus (date-time 1986 10 14) (months 1) (weeks 3))
     #<DateTime 1986-12-05T00:00:00.000Z>

     => (plus (local-date-time 1986 10 14) (months 1) (weeks 3))
     #<LocalDateTime 1986-12-05T00:00:00.000Z>

   An Interval is used to represent the span of time between two DateTime
   instances. Construct one using interval, then query them using within?,
   overlaps?, and abuts?

     => (within? (interval (date-time 1986) (date-time 1990))
                 (date-time 1987))
     true

   To find the amount of time encompassed by an interval, use in-seconds and
   in-minutes:

     => (in-minutes (interval (date-time 1986 10 2) (date-time 1986 10 14)))
     17280

   The overlap function can be used to get an Interval representing the
   overlap between two intervals:

     => (overlap (t/interval (t/date-time 1986) (t/date-time 1990))
                             (t/interval (t/date-time 1987) (t/date-time 1991)))
     #<Interval 1987-01-01T00:00:00.000Z/1990-01-01T00:00:00.000Z>

   Note that all functions in this namespace work with Joda objects or ints. If
   you need to print or parse date-times, see clj-time.format. If you need to
   coerce date-times to or from other types, see clj-time.coerce."
  (:refer-clojure :exclude [extend second])
  (:import
    ;[org.joda.time ReadablePartial ReadableDateTime ReadableInstant
    ;                      ReadablePeriod DateTime DateMidnight
    ;                      DateTimeZone Period PeriodType
    ;                      Interval Years Months Weeks Days Hours Minutes Seconds
    ;                      MutableDateTime DateTimeUtils]
    ;       [org.joda.time.base BaseDateTime]
    [java.time LocalDate LocalTime LocalDateTime ZonedDateTime YearMonth ZoneId
               Duration Instant ZoneOffset Period]
    [java.time.temporal ChronoField TemporalAdjusters TemporalAmount ChronoUnit Temporal]
    [java.time.chrono ChronoZonedDateTime ChronoLocalDateTime ChronoLocalDate]))

;TODO - MSB potentially add some tests around http://joda-time.sourceforge.net/field.html#weekyear

(defn deprecated [message]
  (println "DEPRECATION WARNING: " message))

(defprotocol DateTimeProtocol
  "Interface for various date time functions"
  (year [this] "Return the year component of the given date/time.")
  (month [this]   "Return the month component of the given date/time.")
  (day [this]   "Return the day of month component of the given date/time.")
  (day-of-week [this]   "Return the day of week component of the given date/time. Monday is 1 and Sunday is 7")
  (hour [this]   "Return the hour of day component of the given date/time. A time of 12:01am will have an hour component of 0.")
  (minute [this]   "Return the minute of hour component of the given date/time.")
  (sec [this]   "Return the second of minute component of the given date/time.")
  (second [this]   "Return the second of minute component of the given date/time.")
  (milli [this]   "Return the millisecond of second component of the given date/time.")
  (equal? [this that] "Returns true if ReadableDateTime 'this' is strictly equal to date/time 'that'.")
  (after? [this that] "Returns true if ReadableDateTime 'this' is strictly after date/time 'that'.")
  (before? [this that] "Returns true if ReadableDateTime 'this' is strictly before date/time 'that'.")
  (plus- [this ^TemporalAmount period]
    "Returns a new date/time corresponding to the given date/time moved forwards by the given Period(s).")
  (minus- [this ^TemporalAmount period]
    "Returns a new date/time corresponding to the given date/time moved backwards by the given Period(s).")
  (first-day-of-the-month- [this] "Returns the first day of the month")
  (last-day-of-the-month- [this] "Returns the last day of the month")
  (week-number-of-year [this] "Returns the week of the week based year of the given date/time")
  (week-year [this] "Returns the the week based year of the given date/time."))

(defprotocol InTimeUnitProtocol
  "Interface for in-<time unit> functions"
  (in-millis [this] "Return the time in milliseconds.")
  (in-seconds [this] "Return the time in seconds.")
  (in-minutes [this] "Return the time in minutes.")
  (in-hours [this] "Return the time in hours.")
  (in-days [this] "Return the time in days.")
  (in-weeks [this] "Return the time in weeks")
  (in-months [this] "Return the time in months")
  (in-years [this] "Return the time in years"))

(extend-protocol DateTimeProtocol
  ZonedDateTime
  (year [this] (.getYear this))
  (month [this] (.getMonthValue this))
  (day [this] (.getDayOfMonth this))
  (day-of-week [this] (.getDayOfWeek this))
  (hour [this] (.getHour this))
  (minute [this] (.getMinute this))
  (sec [this]
    {:deprecated "0.6.0"}
    (deprecated "sec is being deprecated in favor of second")
    (.getSecond this))
  (second [this] (.getSecond this))
  (milli [this] (.getLong this ChronoField/MILLI_OF_SECOND))
  (equal? [this ^ChronoZonedDateTime that] (.isEqual this that))
  (after? [this ^ChronoZonedDateTime that] (.isAfter this that))
  (before? [this ^ChronoZonedDateTime that] (.isBefore this that))
  (plus- [this ^TemporalAmount period] (.plus this period))
  (minus- [this ^TemporalAmount period] (.minus this period))
  (first-day-of-the-month- [this]
    (.with this (TemporalAdjusters/firstDayOfMonth)))
  (last-day-of-the-month- [this]
    (.with this (TemporalAdjusters/lastDayOfMonth)))
  (week-number-of-year [this]
    (.get this ChronoField/ALIGNED_WEEK_OF_YEAR))
  (week-year [this] (.get this ChronoField/ALIGNED_WEEK_OF_YEAR))

  LocalDateTime
  (year [this] (.getYear this))
  (month [this] (.getMonthValue this))
  (day [this] (.getDayOfMonth this))
  (day-of-week [this] (.getDayOfWeek this))
  (hour [this] (.getHour this))
  (minute [this] (.getMinute this))
  (sec [this]
    {:deprecated "0.6.0"}
    (deprecated "sec is being deprecated in favor of second")
    (.getSecond this))
  (second [this] (.getSecond this))
  (milli [this] (.getLong this ChronoField/MILLI_OF_SECOND))
  (equal? [this ^ChronoLocalDateTime that] (.isEqual this that))
  (after? [this ^ChronoLocalDateTime that] (.isAfter this that))
  (before? [this ^ChronoLocalDateTime that] (.isBefore this that))
  (plus- [this ^TemporalAmount period] (.plus this period))
  (minus- [this ^TemporalAmount period] (.minus this period))
  (first-day-of-the-month- [this]
    (.with this (TemporalAdjusters/firstDayOfMonth)))
  (last-day-of-the-month- [this]
    (.with this (TemporalAdjusters/lastDayOfMonth)))
  (week-number-of-year [this]
    (.get this ChronoField/ALIGNED_WEEK_OF_YEAR))
  (week-year [this] (.get this ChronoField/ALIGNED_WEEK_OF_YEAR))

  YearMonth
  (year [this] (.getYear this))
  (month [this] (.getMonthValue this))
  (equal? [this ^YearMonth that] (.equals this that))
  (after? [this ^YearMonth that] (.isAfter this that))
  (before? [this ^YearMonth that] (.isBefore this that))
  (plus- [this ^TemporalAmount period] (.plus this period))
  (minus- [this ^TemporalAmount period] (.minus this period))

  LocalDate
  (year [this] (.getYear this))
  (month [this] (.getMonthValue this))
  (day [this] (.getDayOfMonth this))
  (day-of-week [this] (.getDayOfWeek this))
  (equal? [this ^ChronoLocalDate that] (.isEqual this that))
  (after? [this ^ChronoLocalDate that] (.isAfter this that))
  (before? [this ^ChronoLocalDate that] (.isBefore this that))
  (plus- [this ^TemporalAmount period] (.plus this period))
  (minus- [this ^TemporalAmount period] (.minus this period))
  (first-day-of-the-month- [this]
    (.with this (TemporalAdjusters/firstDayOfMonth)))
  (last-day-of-the-month- [this]
    (.with this (TemporalAdjusters/lastDayOfMonth)))
  (week-number-of-year [this]
    (.get this ChronoField/ALIGNED_WEEK_OF_YEAR))
  (week-year [this] (.get this ChronoField/ALIGNED_WEEK_OF_YEAR))

  LocalTime
  (hour [this] (.getHour this))
  (minute [this] (.getMinute this))
  (second [this] (.getSecond this))
  (milli [this] (.getLong this ChronoField/MILLI_OF_SECOND))
  (equal? [this ^LocalTime that] (.equals this that))
  (after? [this ^LocalTime that] (.isAfter this that))
  (before? [this ^LocalTime that] (.isBefore this that))
  (plus- [this ^TemporalAmount period] (.plus this period))
  (minus- [this ^TemporalAmount period] (.minus this period)))

(def ^{:doc "DateTimeZone for UTC."
       :tag ZoneId}
      utc
  (ZoneId/of "UTC"))

(defn ^ZonedDateTime now
  "Returns a DateTime for the current instant in the UTC time zone."
  []
  (ZonedDateTime/now utc))

(defn time-now
  "Returns a LocalTime for the current instant without date or time zone
  using ISOChronology in the current time zone."
  []
  (LocalTime/now))

(defn ^{:deprecated "0.12.0"} today-at-midnight
  "DEPRECATED: Please use with-time-at-start-of-day instead. See http://goo.gl/nQCmKd
  Returns a DateMidnight for today at midnight in the UTC time zone."
  ([] (today-at-midnight utc))
  ([tz]
   (let [^ZoneId zid (cond-> tz (string? tz) ZoneId/of)]
     (ZonedDateTime/of
       (LocalDateTime/of (LocalDate/now) LocalTime/MIDNIGHT)
       zid))))

(defn ^ZonedDateTime with-time-at-start-of-day
  "Returns a DateTime representing the start of the day. Normally midnight,
  but not always true, as in some time zones with daylight savings."
  [^ZonedDateTime dt]
  (-> dt .toLocalDate (.atStartOfDay (.getZone dt))))

(defn epoch
  "Returns a DateTime for the beginning of the Unix epoch in the UTC time zone."
  []
  (ZonedDateTime/ofInstant Instant/EPOCH utc))

(defn date-midnight
  "Constructs and returns a new DateMidnight in UTC.
   Specify the year, month of year, day of month. Note that month and day are
   1-indexed. Any number of least-significant components can be ommited, in which case
   they will default to 1."
  ([year]
    (date-midnight year 1 1))
  ([^long year ^long month]
    (date-midnight year month 1))
  ([^Long year ^Long month ^Long day]
    (ZonedDateTime/of year month day 0 0 0 0 utc)))

(defn min-date
  "Minimum of the provided DateTimes."
  [dt & dts]
  (reduce #(if (before? %1 %2) %1 %2) dt dts))

(defn max-date
  "Maximum of the provided DateTimes."
  [dt & dts]
  (reduce #(if (after? %1 %2) %1 %2) dt dts))

(defn ^ZonedDateTime date-time
  "Constructs and returns a new DateTime in UTC.
   Specify the year, month of year, day of month, hour of day, minute of hour,
   second of minute, and millisecond of second. Note that month and day are
   1-indexed while hour, second, minute, and millis are 0-indexed.
   Any number of least-significant components can be ommited, in which case
   they will default to 1 or 0 as appropriate."
  ([year]
   (date-time year 1 1 0 0 0 0))
  ([year month]
   (date-time year month 1 0 0 0 0))
  ([year month day]
   (date-time year month day 0 0 0 0))
  ([year month day hour]
   (date-time year month day hour 0 0 0))
  ([year month day hour minute]
   (date-time year month day hour minute 0 0))
  ([year month day hour minute second]
   (date-time year month day hour minute second 0))
  ([^Integer year ^Integer month ^Integer day ^Integer hour
    ^Integer minute ^Integer second ^Integer millis]
   (ZonedDateTime/of year month day hour minute second ^Integer (* 1000 millis) ^DateTimeZone utc)))

(defn ^LocalDateTime local-date-time
  "Constructs and returns a new LocalDateTime.
   Specify the year, month of year, day of month, hour of day, minute of hour,
   second of minute, and millisecond of second. Note that month and day are
   1-indexed while hour, second, minute, and millis are 0-indexed.
   Any number of least-significant components can be omitted, in which case
   they will default to 1 or 0 as appropriate."
  ([year]
   (local-date-time year 1 1 0 0 0 0))
  ([year month]
   (local-date-time year month 1 0 0 0 0))
  ([year month day]
   (local-date-time year month day 0 0 0 0))
  ([year month day hour]
   (local-date-time year month day hour 0 0 0))
  ([year month day hour minute]
   (local-date-time year month day hour minute 0 0))
  ([year month day hour minute second]
   (local-date-time year month day hour minute second 0))
  ([^Integer year ^Integer month ^Integer day ^Integer hour
    ^Integer minute ^Integer second ^Integer millis]
   (LocalDateTime/of year month day hour minute second ^Integer (* 1000 millis))))

(defn ^YearMonth year-month
  "Constructs and returns a new YearMonth.
   Specify the year and month of year. Month is 1-indexed and defaults
   to January (1)."
  ([year]
     (year-month year 1))
  ([^Integer year ^Integer month]
     (YearMonth/of year month)))

(defn ^LocalDate local-date
  "Constructs and returns a new LocalDate.
   Specify the year, month, and day. Does not deal with timezones."
  [^Integer year ^Integer month ^Integer day]
  (LocalDate/of year month day))

(defn ^LocalTime local-time
  "Constructs and returns a new LocalTime.
   Specify the hour of day, minute of hour, second of minute, and millisecond of second.
   Any number of least-significant components can be ommited, in which case
   they will default to 1 or 0 as appropriate."
  ([hour]
   (local-time hour 0 0 0))
  ([hour minute]
   (local-time hour minute 0 0))
  ([hour minute second]
   (local-time hour minute second 0))
  ([^Integer hour ^Integer minute ^Integer second ^Integer millis]
   (LocalTime/of hour minute second (* 1000 millis)))
  )

(defn ^LocalDate today
  "Constructs and returns a new LocalDate representing today's date.
   LocalDate objects do not deal with timezones at all."
  []
  (LocalDate/now))

(defn time-zone-for-offset
  "Returns a DateTimeZone for the given offset, specified either in hours or
   hours and minutes."
  ([hours]
   (ZoneOffset/ofHours hours))
  ([hours minutes]
   (ZoneOffset/ofHoursMinutes hours minutes))
  ([hours minutes seconds]
   (ZoneOffset/ofHoursMinutesSeconds hours minutes seconds)))

(defn time-zone-for-id
  "Returns a DateTimeZone for the given ID, which must be in long form, e.g.
   'America/Matamoros'."
  [^String id]
  (ZoneOffset/of id))

(defn available-ids
  "Returns a set of available IDs for use with time-zone-for-id."
  []
  (ZoneId/getAvailableZoneIds))

(defn ^ZoneId default-time-zone
  "Returns the default DateTimeZone for the current environment."
  []
  (ZoneId/systemDefault))

(defn ^ZonedDateTime
  to-time-zone
  "Returns a new ReadableDateTime corresponding to the same absolute instant in time as
   the given ReadableDateTime, but with calendar fields corresponding to the given
   TimeZone."
  [^ZonedDateTime dt ^ZoneId tz]
  (.withZoneSameInstant dt tz))

(defn ^ZonedDateTime
  from-time-zone
  "Returns a new ReadableDateTime corresponding to the same point in calendar time as
   the given ReadableDateTime, but for a correspondingly different absolute instant in
   time."
  [^ZonedDateTime dt ^ZoneId tz]
  (.withZoneSameLocal dt tz))

(defn ^TemporalAmount years
  "Given a number, returns a Period representing that many years.
   Without an argument, returns a PeriodType representing only years."
  ([]
     ChronoUnit/YEARS)
  ([^Integer n]
   (Period/ofYears n)))

(defn ^TemporalAmount months
  "Given a number, returns a Period representing that many months.
   Without an argument, returns a PeriodType representing only months."
  ([]
     ChronoUnit/MONTHS)
  ([^Integer n]
     (Period/ofMonths n)))

(defn ^TemporalAmount weeks
  "Given a number, returns a Period representing that many weeks.
   Without an argument, returns a PeriodType representing only weeks."
  ([]
     ChronoUnit/WEEKS)
  ([^Integer n]
     (Period/ofWeeks n)))

(defn ^TemporalAmount days
  "Given a number, returns a Period representing that many days.
   Without an argument, returns a PeriodType representing only days."
  ([]
     ChronoUnit/DAYS)
  ([^Integer n]
     (Period/ofDays n)))

(defn ^TemporalAmount hours
  "Given a number, returns a Period representing that many hours.
   Without an argument, returns a PeriodType representing only hours."
  ([]
     ChronoUnit/HOURS)
  ([^Integer n]
     (Duration/of n (hours))))

(defn ^TemporalAmount minutes
  "Given a number, returns a Period representing that many minutes.
   Without an argument, returns a PeriodType representing only minutes."
  ([]
     ChronoUnit/MINUTES)
  ([^Integer n]
     (Duration/of n (minutes))))

(defn ^TemporalAmount seconds
  "Given a number, returns a Period representing that many seconds.
   Without an argument, returns a PeriodType representing only seconds."
  ([]
     ChronoUnit/SECONDS)
  ([^Integer n]
     (Duration/of n (seconds))))

(extend-protocol InTimeUnitProtocol
  TemporalAmount
  (in-millis [this] (.get this ChronoUnit/MILLIS))
  (in-seconds [this] (.get this ChronoUnit/SECONDS))
  (in-minutes [this] (.get this ChronoUnit/MINUTES))
  (in-hours [this] (.get this ChronoUnit/HOURS))
  (in-days [this] (.get this ChronoUnit/DAYS))
  (in-weeks [this] (.get this ChronoUnit/WEEKS))
  (in-months [this] (.get this ChronoUnit/WEEKS))
  (in-years [this] (.get this ChronoUnit/YEARS)))

(defrecord Interval [^Temporal start ^Temporal end])

(defn in-msecs
  "DEPRECATED: Returns the number of milliseconds in the given Interval."
  {:deprecated "0.6.0"}
  [{:keys [start end]}]
  (deprecated "in-msecs has been deprecated in favor of in-millis")
  (in-millis (Duration/between start end)))

(defn in-secs
  "DEPRECATED: Returns the number of standard seconds in the given Interval."
  {:deprecated "0.6.0"}
  [{:keys [start end]}]
  (deprecated "in-secs has been deprecated in favor of in-seconds")
  (in-seconds (Duration/between start end)))

(defn secs
  "DEPRECATED"
  {:deprecated "0.6.0"}
  ([]
     (deprecated "secs has been deprecated in favor of seconds")
     (seconds))
  ([^Integer n]
     (deprecated "secs has been deprecated in favor of seconds")
     (seconds n)))

(defn millis
  "Given a number, returns a Period representing that many milliseconds.
   Without an argument, returns a PeriodType representing only milliseconds."
  ([] ChronoUnit/MILLIS)
  ([n] (Duration/ofMillis n)))

(defn plus
  "Returns a new date/time corresponding to the given date/time moved forwards by
   the given Period(s)."
  ([dt ^TemporalAmount p]
     (plus- dt p))
  ([dt p & ps]
     (reduce plus- (plus- dt p) ps)))

(defn minus
  "Returns a new date/time object corresponding to the given date/time moved backwards by
   the given Period(s)."
  ([dt ^TemporalAmount p]
   (minus- dt p))
  ([dt p & ps]
     (reduce minus- (minus- dt p) ps)))

(defn ago
  "Returns a DateTime a supplied period before the present.
  e.g. (-> 5 years ago)"
  [^TemporalAmount period]
  (minus (now) period))

(defn yesterday
  "Returns a DateTime for yesterday relative to now"
  []
  (-> 1 days ago))

(defn from-now
  "Returns a DateTime a supplied period after the present.
  e.g. (-> 30 minutes from-now)"
  [^TemporalAmount period]
  (plus (now) period))

(defn earliest
  "Returns the earliest of the supplied DateTimes"
  ([dt1 dt2]
     (if (before? dt1 dt2) dt1 dt2))
  ([dts]
   (reduce earliest dts)))

(defn latest
  "Returns the latest of the supplied DateTimes"
  ([dt1 dt2]
     (if (after? dt1 dt2) dt1 dt2))
  ([dts]
     (reduce latest dts)))

(defn interval
  "Returns an interval representing the span between the two given ReadableDateTimes.
   Note that intervals are closed on the left and open on the right."
  [dt-a dt-b]
  (Interval. dt-a dt-b))

(defn start
  "Returns the start DateTime of an Interval."
  [in]
  (:start in))

(defn end
  "Returns the end DateTime of an Interval."
  [in]
  (:end in))

(defn extend
  "Returns an Interval with an end ReadableDateTime the specified Period after the end
   of the given Interval"
  [in & by]
  (update in :end (fn [e] (apply plus e by))))

(defn adjust
  "Returns an Interval with the start and end adjusted by the specified Periods."
  [in & by]
  (interval (apply plus (start in) by)
            (apply plus (end in) by)))

(comment
  (from-now (days 4))
  (interval (now) (from-now (days 4)))
  (adjust (interval (now) (from-now (days 4))) (hours -4) (minutes 30)))

(defn within?
  "With 2 arguments: Returns true if the given Interval contains the given
   ReadableDateTime. Note that if the ReadableDateTime is exactly equal to the
   end of the interval, this function returns false.
   With 3 arguments: Returns true if the start ReadablePartial is
   equal to or before and the end ReadablePartial is equal to or after the test
   ReadablePartial."
  ([{:keys [start end]} t]
   (or (equal? start t)
       (and (before? start t) (after? end t))))
  ([start end test]
     (or (equal? start test)
         (equal? end test)
         (and (before? start test) (after? end test)))))

(defn overlaps?
  "With 2 arguments: Returns true of the two given Intervals overlap.
   Note that intervals that satisfy abuts? do not satisfy overlaps?
   With 4 arguments: Returns true if the range specified by start-a and end-a
   overlaps with the range specified by start-b and end-b."
  ([{si :start ei :end} {sf :start ef :end}]
   (and (after? ei sf) (before? si ef)))
  ([start-a end-a start-b end-b]
     (or (and (before? start-b end-a) (after? end-b start-a))
         (and (after? end-b start-a) (before? start-b end-a))
         (or (equal? start-a end-b) (equal? start-b end-a)))))

(defn overlap
  "Returns an Interval representing the overlap of the specified Intervals.
   Returns nil if the Intervals do not overlap.
   The first argument must not be nil.
   If the second argument is nil then the overlap of the first argument
   and a zero duration interval with both start and end times equal to the
   current time is returned."
  [^Interval i-a ^Interval i-b]
     ;; joda-time AbstractInterval.overlaps:
     ;;    null argument means a zero length interval 'now'.
     (cond (nil? i-b) (let [n (now)] (overlap i-a (interval n n)))
           (overlaps? i-a i-b) (interval (latest (start i-a) (start i-b))
                                         (earliest (end i-a) (end i-b)))
           :else nil))

(defn abuts?
  "Returns true if Interval i-a abuts i-b, i.e. then end of i-a is exactly the
   beginning of i-b."
  [{:keys [end]} {:keys [start]}]
  (equal? end start))

(defn mins-ago
  [d]
  (in-minutes (interval d (now))))

(defn first-day-of-the-month
  ([^long year ^long month]
     (first-day-of-the-month- (date-time year month)))
  ([dt]
     (first-day-of-the-month- dt)))

(defn last-day-of-the-month
  ([^long year ^long month]
     (last-day-of-the-month- (date-time year month)))
  ([dt]
     (last-day-of-the-month- dt)))

(defn number-of-days-in-the-month
  (^long [dt]
         (day (last-day-of-the-month- dt)))
  (^long [^long year ^long month]
         (day (last-day-of-the-month- (date-time year month)))))

(defn nth-day-of-the-month
  "Returns the nth day of the month."
  ([^long year ^long month ^long n]
   (nth-day-of-the-month (date-time year month) n))
  ([dt n]
   (plus (first-day-of-the-month dt)
         (days (- n 1)))))

(defn today-at
  ([hours minutes seconds millis tz]
   (ZonedDateTime/of
     (today)
     (local-time hours minutes seconds millis)
     tz))
  ([hours minutes seconds millis]
   (today-at hours minutes seconds millis utc))
  ([hours minutes seconds]
     (today-at hours minutes seconds 0))
  ([hours minutes]
     (today-at hours minutes 0)))

;TODO - MSB - consider using Clock/fixed instead
;(defn do-at* [^BaseDateTime base-date-time body-fn]
;  (DateTimeUtils/setCurrentMillisFixed (.getMillis base-date-time))
;  (try
;    (body-fn)
;    (finally
;      (DateTimeUtils/setCurrentMillisSystem))))
;
;(defmacro do-at
;  "Like clojure.core/do except evalautes the expression at the given date-time"
;  [^BaseDateTime base-date-time & body]
;  `(do-at* ~base-date-time
;    (fn [] ~@body)))

(defn floor
  "Floors the given date-time dt to the given time unit dt-fn,
  e.g. (floor (now) hour) returns (now) for all units
  up to and including the hour"
  ([t dt-fn]
   (let [getters [year month day hour minute second milli]
         setters [years months days hours minutes seconds millis]
         dt-fns (reverse (map vector getters setters))]
     (reduce
       (fn [t [g s]]
         (prn [(= dt-fn g) s])
         (let [x (minus t (s (g t)))]
           (if (= dt-fn g) (reduced x) x)))
       t dt-fns))))

;(defmacro ^:private when-available [sym & body]
;  (when (resolve sym)
;    `(do ~@body)))

;(when-available Inst
;  (extend-protocol Inst
;    org.joda.time.ReadableInstant
;    (inst-ms* [inst]
;      (.getMillis inst))))
