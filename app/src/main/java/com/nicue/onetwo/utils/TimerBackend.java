package com.nicue.onetwo.utils;

import java.util.Locale;

public class TimerBackend {
    /**
     * Formats remaining time for display.
     * If time is above threshold, shows M:SS.
     * If time is below threshold, shows M:SS:DD (deciseconds).
     */
    public static String formatRemainingTime(long milliseconds, long panicThresholdMs) {
        long totalSeconds = milliseconds / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;

        if (milliseconds >= panicThresholdMs) {
            return String.format(Locale.US, "%d:%02d", minutes, seconds);
        } else {
            long deciseconds = (milliseconds % 1000L) / 10L;
            return String.format(Locale.US, "%d:%02d:%02d", minutes, seconds, deciseconds);
        }
    }
}
