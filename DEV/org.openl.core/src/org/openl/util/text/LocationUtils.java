package org.openl.util.text;

public final class LocationUtils {
    private LocationUtils() {
    }

    public static TextInterval createTextInterval(int start, int end) {
        return new TextInterval(new AbsolutePosition(start), new AbsolutePosition(end));
    }

    public static TextInterval createTextInterval(String text) {
        return createTextInterval(0, text.length());
    }
}
