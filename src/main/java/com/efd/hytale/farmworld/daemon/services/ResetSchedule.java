package com.efd.hytale.farmworld.daemon.services;

import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

public final class ResetSchedule {
    private ResetSchedule() {}

    public static Instant computeNextReset(Instant lastReset, int intervalDays, String resetAtPolicy, ZoneId zone) {
        if (intervalDays <= 0) intervalDays = 7;

        if (resetAtPolicy == null || resetAtPolicy.isBlank()) {
            return lastReset.plus(Duration.ofDays(intervalDays));
        }

        Policy p = parsePolicy(resetAtPolicy);
        if (p == null) return lastReset.plus(Duration.ofDays(intervalDays));

        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime candidate = now.with(TemporalAdjusters.nextOrSame(p.dayOfWeek))
                .withHour(p.hour).withMinute(p.minute).withSecond(0).withNano(0);

        if (candidate.isBefore(now)) candidate = candidate.plusWeeks(1);

        Instant min = lastReset.plus(Duration.ofHours(1));
        Instant cand = candidate.toInstant();
        if (cand.isBefore(min)) cand = min;
        return cand;
    }

    public static String describePolicy(String policy) {
        Policy p = parsePolicy(policy);
        if (p == null) return policy;
        return p.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + String.format("%02d:%02d", p.hour, p.minute);
    }

    private record Policy(DayOfWeek dayOfWeek, int hour, int minute) {}

    private static Policy parsePolicy(String s) {
        try {
            String[] parts = s.trim().split("\\s+");
            if (parts.length != 2) return null;
            DayOfWeek dow = switch (parts[0].toUpperCase(Locale.ROOT)) {
                case "MON" -> DayOfWeek.MONDAY;
                case "TUE" -> DayOfWeek.TUESDAY;
                case "WED" -> DayOfWeek.WEDNESDAY;
                case "THU" -> DayOfWeek.THURSDAY;
                case "FRI" -> DayOfWeek.FRIDAY;
                case "SAT" -> DayOfWeek.SATURDAY;
                case "SUN" -> DayOfWeek.SUNDAY;
                default -> null;
            };
            if (dow == null) return null;
            String[] hm = parts[1].split(":");
            if (hm.length != 2) return null;
            int h = Integer.parseInt(hm[0]);
            int m = Integer.parseInt(hm[1]);
            if (h < 0 || h > 23 || m < 0 || m > 59) return null;
            return new Policy(dow, h, m);
        } catch (Exception e) {
            return null;
        }
    }
}
