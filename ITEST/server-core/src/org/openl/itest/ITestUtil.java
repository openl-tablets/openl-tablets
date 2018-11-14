package org.openl.itest;

public final class ITestUtil {

    private ITestUtil() {
        /* NON */
    }

    public static String cleanupXml(String s) {
        return s.replaceAll("\\\\\"", "\"")
                .replaceAll(">(\\\\n|\\n)\\s*<", "><")
                .replaceFirst("^\"(.+)\"$", "$1");
    }

}
