/*
 * Created on May 25, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.util.conf;

import org.openl.util.StringUtils;

/**
 * @author snshor
 */
public class Version implements Comparable<Version> {

    private class Parser {
        private int pos;
        private final String s;

        private Parser(String s, int from) {
            this.s = s;
            this.pos = from;
        }

        private int getInt() {
            var len = s.length();
            if (pos >= len || !Character.isDigit(s.charAt(pos))) {
                return -1;
            }

            var n = 0;
            for (; pos < len; pos++) {
                var c = s.charAt(pos);
                if (Character.isDigit(c)) {
                    n = n * 10 + c - '0';
                } else {
                    break;
                }
            }
            ++pos;
            return n;
        }

        void parseVersion() {
            if ((version[MAJOR] = getInt()) >= 0 && (version[MINOR] = getInt()) >= 0) {
                version[VARIANT] = getInt();
            }
            version[BUILD] = getInt();
        }

    }

    private static final int MAJOR = 0;
    private static final int MINOR = 1;
    private static final int VARIANT = 2;
    private static final int BUILD = 3;

    private final int[] version = new int[]{-1, -1, -1, -1};
    private final String pattern;

    private static int calcNumbersSeparatedByDots(String s, int from, String pattern) {
        var nnum = 0;
        var len = s.length();
        var inNumber = false;

        for (var i = from; i < len && nnum <= pattern.length(); ++i) {
            var c = s.charAt(i);
            if (Character.isDigit(c)) {
                if (!inNumber) {
                    ++nnum;
                    inNumber = true;
                }
            } else if (inNumber && c == pattern.charAt(nnum - 1)) {
                inNumber = false;
            } else {
                break;
            }
        }
        return nnum;
    }

    private static boolean isVersion(String s, int from, String pattern) {
        if (StringUtils.isBlank(s)) {
            return false;
        }
        return calcNumbersSeparatedByDots(s, from, pattern) == pattern.length() + 1;
    }

    public static Version parseVersion(String s, int from, String pattern) {
        if (!isVersion(s, from, pattern)) {
            throw new IllegalArgumentException("This is not a valid version: " + s.substring(from) + " in " + s);
        }

        var v = new Version(pattern);
        v.parseIn(s, from);
        return v;
    }

    private Version(String pattern) {
        this.pattern = pattern;
    }

    @Override
    public int compareTo(Version v) {

        for (var i = 0; i < version.length; i++) {
            if (version[i] != v.version[i]) {
                return Integer.compare(version[i], v.version[i]);
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Version v)) {
            return false;
        }
        for (var i = 0; i < version.length; i++) {
            if (version[i] != v.version[i]) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        return version[BUILD] * 119 + version[VARIANT] * 37 + version[MINOR] * 17 + version[MAJOR];
    }

    private void parseIn(String s, int from) {
        new Parser(s, from).parseVersion();
    }

    @Override
    public String toString() {
        var buf = new StringBuilder();
        for (var i = 0; i < version.length; i++) {
            if (version[i] >= 0) {
                buf.append(version[i]);
            } else {
                break;
            }
            if (i < pattern.length()) {
                buf.append(pattern.charAt(i));
            }
        }
        return buf.toString();
    }

}
