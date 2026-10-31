package io.github.x71x7.xparkour;

import java.util.Locale;

public final class TimeUtil {

    private TimeUtil() {}

    public static String format(long ms) {
        if (ms < 0) ms = 0;
        long minutes = ms / 60_000;
        long seconds = (ms / 1000) % 60;
        long millis = ms % 1000;
        if (minutes > 0) {
            return String.format(Locale.ROOT, "%d:%02d.%03d", minutes, seconds, millis);
        }
        return String.format(Locale.ROOT, "%d.%03d", seconds, millis);
    }
}
