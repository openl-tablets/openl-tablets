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

    public interface IVersionStartPatternFinder {
        /**
         *
         * @param s
         * @param previousStart index of previously found start or -1 if this is first
         * @return index of possible start for version or -1
         */
        int findVersionStart(String s, int previousStart);
    }

    class Parser {
        int pos;
        String s;

        Parser(String s, int from) {
            this.s = s;
            pos = from;
        }

        int getInt() {
            int len = s.length();

            if (pos >= len || !Character.isDigit(s.charAt(pos))) {
                return -1;
            }

            int n = 0;
            for (; pos < len; pos++) {
                char c = s.charAt(pos);
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
            if ((version[MAJOR] = getInt()) >= 0) {
                if ((version[MINOR] = getInt()) >= 0) {
                    if ((version[VARIANT] = getInt()) >= 0) {

                    }
                }
            }
            version[BUILD] = getInt();
        }

    }

    static public class StandardVersionStartPatternFinder implements IVersionStartPatternFinder {

        @Override
        public int findVersionStart(String s, int previousStart) {
            if (previousStart == -1) // just first search is a real one
            {
                int idx = s.lastIndexOf('_');
                if (idx >= 0) {
                    return idx + 1;
                }
            }
            return -1;
        }

    }

    public static final int MAJOR = 0, MINOR = 1, VARIANT = 2, BUILD = 3;

    public static final String JAVA_VERSION_PATTERN = ".._";

    int[] version = new int[] { -1, -1, -1, -1 };

    String pattern = JAVA_VERSION_PATTERN;

    public static int calcNumbersSeparatedByDots(String s, int from, String pattern) {
        int nnum = 0;

        int len = s.length();

        boolean inNumber = false;

        for (int i = from; i < len && nnum <= pattern.length(); ++i) {
            char c = s.charAt(i);

            if (inNumber) {
                if (Character.isDigit(c)) {
                    continue;
                }
                inNumber = false;
                if (c == pattern.charAt(nnum - 1)) {
                    continue;
                }
                break;
            }
            if (Character.isDigit(c)) {
                ++nnum;
                inNumber = true;
                continue;
            }
            break;

        }
        return nnum;
    }

    /**
     * @param vx
     * @return
     */
    public static Version extractVersion(String vx) throws Exception {
        return extractVersion(vx, JAVA_VERSION_PATTERN);
    }

    static public Version extractVersion(String s, IVersionStartPatternFinder finder, String pattern) throws Exception {
        int idx = findVersionStart(s, finder, pattern);
        if (idx == -1) {
            throw new Exception("Could not find version pattern in " + s);
        }

        return parseVersion(s, idx, pattern);
    }

    public static Version extractVersion(String s, String pattern) throws Exception {
        return extractVersion(s, new StandardVersionStartPatternFinder(), pattern);
    }

    public static int findVersionStart(String s, IVersionStartPatternFinder finder, String pattern) {
        int idx = -1;
        int loopProtector = 0;

        while (true) {
            idx = finder.findVersionStart(s, idx);
            if (idx == -1) {
                return -1;
            }

            if (isVersion(s, idx, pattern)) {
                return idx;
            }

            if (++loopProtector > 100) {
                throw new RuntimeException("Check implementation of Your Pattern Finder");
            }

        }
    }

    public static int findVersionStart(String s, String pattern) {
        return findVersionStart(s, new StandardVersionStartPatternFinder(), pattern);
    }

    /**
     * @param t1
     * @param i
     * @return
     */
    public static boolean isVersion(String t1, int i) {
        return isVersion(t1, i, JAVA_VERSION_PATTERN);
    }

    public static boolean isVersion(String s, int from, String pattern) {
        if (StringUtils.isBlank(s)) {
            return false;
        }
        return calcNumbersSeparatedByDots(s, from, pattern) == pattern.length() + 1;
    }

    /**
     * @param s
     * @param i
     * @return
     */
    public static Version parseVersion(String s, int i) {
        return parseVersion(s, i, JAVA_VERSION_PATTERN);
    }

    static public Version parseVersion(String s, int from, String pattern) {
        if (!isVersion(s, from, pattern)) {
            throw new RuntimeException("This is not a valid version: " + s.substring(from) + " in " + s);
        }

        Version v = new Version();
        v.setPattern(pattern);

        v.parseIn(s, from);
        return v;

    }

    public Version() {
    }

    public Version(int major, int minor, int variant, int build, String pattern) {
        version[MAJOR] = major;
        version[MINOR] = minor;
        version[VARIANT] = variant;
        version[BUILD] = build;
        if (pattern != null) {
            this.pattern = pattern;
        }
    }

    @Override
    public int compareTo(Version v) {

        for (int i = 0; i < version.length; i++) {
            if (version[i] != v.version[i]) {
                return version[i] - v.version[i];
            }
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Version)) {
            return false;
        }
        Version v = (Version) obj;
        for (int i = 0; i < version.length; i++) {
            if (version[i] != v.version[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return
     */
    public int getBuild() {
        return version[BUILD];
    }

    /**
     * @return
     */
    public int getMajor() {
        return version[MAJOR];
    }

    /**
     * @return
     */
    public int getMinor() {
        return version[MINOR];
    }

    /**
     * @return
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @return
     */
    public int getVariant() {
        return version[VARIANT];
    }

    @Override
    public int hashCode() {
        return version[BUILD] * 119 + version[VARIANT] * 37 + version[MINOR] * 17 + version[MAJOR];
    }

    /**
     * @param s
     * @param from
     */
    private void parseIn(String s, int from) {
        new Parser(s, from).parseVersion();
    }

    /**
     * @param i
     */
    public void setBuild(int i) {
        version[BUILD] = i;
    }

    /**
     * @param i
     */
    public void setMajor(int i) {
        version[MAJOR] = i;
    }

    /**
     * @param i
     */
    public void setMinor(int i) {
        version[MINOR] = i;
    }

    /**
     * @param string
     */
    public void setPattern(String string) {
        pattern = string;
    }

    /**
     * @param i
     */
    public void setVariant(int i) {
        version[VARIANT] = i;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        for (int i = 0; i < version.length; i++) {
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
