package org.openl.util;

import java.util.Locale;

/**
 * Util class to get operating system information.
 */
public class OS {

    private OS() {
    }

    private static final String OS = System.getProperty("os.name", "unknown").toLowerCase(Locale.ROOT);

    public static boolean isWindows() {
        return OS.contains("win");
    }

    public static boolean isMac() {
        return OS.contains("mac");
    }

    public static boolean isUnix() {
        return OS.contains("nux");
    }
}
