package edu.univ.erp.ui.timetable;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TimeSlotParser {

  private static final Logger log = LoggerFactory.getLogger(TimeSlotParser.class);

  private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("H:mm");

  public static class Parsed {
    public final List<String> days;
    public final LocalTime start;
    public final LocalTime end;

    public Parsed(List<String> days, LocalTime start, LocalTime end) {
      this.days = days;
      this.start = start;
      this.end = end;
    }
  }

  public static Parsed parse(String dayTime) {
    log.debug("Parsing time slot: '{}'", dayTime);

    if (dayTime == null || dayTime.isBlank()) {
      log.error("Failed to parse empty day_time string");
      throw new IllegalArgumentException("Empty day_time");
    }

    String[] parts = dayTime.trim().split("\\s+", 2);
    if (parts.length < 2) {
      log.error("Invalid day_time format: {}", dayTime);
      throw new IllegalArgumentException("Invalid day_time format: " + dayTime);
    }

    String daysPart = parts[0];
    String timePart = parts[1];

    log.debug("Days part='{}', Time part='{}'", daysPart, timePart);

    String[] dayTokens = daysPart.split("[/,:;]");
    List<String> days = new ArrayList<>();

    for (String d : dayTokens) {
      String dd = d.trim();
      if (!dd.isEmpty()) {
        String norm = normalizeDay(dd);
        log.debug("Normalized day '{}' -> '{}'", dd, norm);
        days.add(norm);
      }
    }

    String[] times = timePart.split("-");
    if (times.length != 2) {
      log.error("Time range missing for '{}'", dayTime);
      throw new IllegalArgumentException("Time range missing: " + dayTime);
    }

    LocalTime start = parseTimeToken(times[0].trim());
    LocalTime end = parseTimeToken(times[1].trim());

    log.debug("Parsed start='{}', end='{}'", start, end);

    if (end.isBefore(start) || end.equals(start)) {
      log.error("End time '{}' is not after start time '{}' for '{}'", end, start, dayTime);
      throw new IllegalArgumentException("End time must be after start time: " + dayTime);
    }

    log.info("Successfully parsed time slot '{}': days={}, start={}, end={}", 
             dayTime, days, start, end);

    return new Parsed(days, start, end);
  }

  private static String normalizeDay(String raw) {
    raw = raw.trim();
    String lower = raw.toLowerCase();
    if (lower.startsWith("mon"))
      return "Mon";
    if (lower.startsWith("tue"))
      return "Tue";
    if (lower.startsWith("wed"))
      return "Wed";
    if (lower.startsWith("thu"))
      return "Thu";
    if (lower.startsWith("fri"))
      return "Fri";
    if (lower.startsWith("sat"))
      return "Sat";
    if (lower.startsWith("sun"))
      return "Sun";
    return raw;
  }

  private static LocalTime parseTimeToken(String token) {
    String t = token.trim();
    if (!t.contains(":"))
      t = t + ":00";

    LocalTime lt = LocalTime.parse(t, TIME_FMT);
    log.debug("Parsed time token '{}' -> '{}'", token, lt);

    return lt;
  }
}
