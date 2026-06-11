package com.example.nspush;

import java.text.DateFormat;
import java.util.Date;

final class NightscoutEntry {
    final int sgv;
    final long timestamp;
    final String direction;

    NightscoutEntry(int sgv, long timestamp, String direction) {
        this.sgv = sgv;
        this.timestamp = timestamp;
        this.direction = direction == null ? "" : direction;
    }

    String formattedTime() {
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date(timestamp));
    }

    String trendArrow() {
        switch (direction) {
            case "DoubleUp":
                return "⇈";
            case "SingleUp":
                return "↑";
            case "FortyFiveUp":
                return "↗";
            case "Flat":
                return "→";
            case "FortyFiveDown":
                return "↘";
            case "SingleDown":
                return "↓";
            case "DoubleDown":
                return "⇊";
            default:
                return "";
        }
    }

    boolean isStale(long nowMillis) {
        return nowMillis - timestamp > 15L * 60L * 1000L;
    }
}
