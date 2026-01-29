package com.efd.hytale.farmworld.shared.commands;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class CommandMessages {
  public static final String PREFIX = "[FarmWorld] ";

  private CommandMessages() {}

  public static String success(String message) {
    return PREFIX + "[OK] " + message;
  }

  public static String info(String message) {
    return PREFIX + "[INFO] " + message;
  }

  public static String error(String message) {
    return PREFIX + "[FEHLER] " + message;
  }

  public static String prefixLines(List<String> lines, String label) {
    return lines.stream()
        .map(line -> PREFIX + label + line)
        .collect(Collectors.joining("\n"));
  }

  public static String formatCoordinate(double value) {
    long rounded = Math.round(value);
    if (Math.abs(value - rounded) < 0.0001) {
      return String.valueOf(rounded);
    }
    return String.format(Locale.ROOT, "%.2f", value);
  }

  public static String formatDurationSeconds(long seconds) {
    if (seconds <= 0L) {
      return "0s";
    }
    long minutes = seconds / 60;
    long remainingSeconds = seconds % 60;
    if (minutes <= 0) {
      return seconds + "s";
    }
    long hours = minutes / 60;
    long remainingMinutes = minutes % 60;
    if (hours <= 0) {
      return minutes + "m " + remainingSeconds + "s";
    }
    return hours + "h " + remainingMinutes + "m " + remainingSeconds + "s";
  }

  public static String formatSecondsUntil(long epochSeconds) {
    if (epochSeconds <= 0L) {
      return "unbekannt";
    }
    long seconds = Duration.between(Instant.now(), Instant.ofEpochSecond(epochSeconds)).getSeconds();
    return formatDurationSeconds(Math.max(0L, seconds));
  }
}
