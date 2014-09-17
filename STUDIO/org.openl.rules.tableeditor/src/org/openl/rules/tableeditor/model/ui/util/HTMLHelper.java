 package org.openl.rules.tableeditor.model.ui.util;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Andrei Astrouski
 */
public final class HTMLHelper {

    private HTMLHelper() {
    }

    public static String htmlStringWithSelections(String src, String[] tokens) {
        StringHighlighter sf = new StringHighlighter(tokens, src);        
        return sf.highlightStringsInText();
    }

    private static String toHex(short x) {
        String s = Integer.toHexString(x);

        switch (s.length()) {
            case 1:
                return "0" + s;
            case 2:
                return s;
        }
        return s.substring(s.length() - 2);
    }

    public static String toHexColor(short[] x) {
        if (x == null) {
            return "#000";
        }

        String hex1 = toHex(x[0]);
        String hex2 = toHex(x[1]);
        String hex3 = toHex(x[2]);

        boolean dig3hex = (hex1.charAt(0) == hex1.charAt(1))
                       && (hex2.charAt(0) == hex2.charAt(1))
                       && (hex3.charAt(0) == hex3.charAt(1));

        return new StringBuilder()
            .append("#")
            .append(dig3hex ? hex1.charAt(0) : hex1)
            .append(dig3hex ? hex2.charAt(0) : hex2)
            .append(dig3hex ? hex3.charAt(0) : hex3)
            .toString();
    }

    public static String toRgbColor(short[] color) {
        return "rgb(" + String.valueOf(color[0]) + "," + String.valueOf(color[1]) + "," + String.valueOf(color[2])
                + ")";
    }

    public static String urlLink(String url, String title, String htmltext, String target, String classes) {
        String s1 = "<a href=\"" + url + "\"";
        if (title != null) {
            s1 += (" title=\"" + title + "\"");
        }
        if (target != null) {
            s1 += (" target=\"" + target + "\"");
        }
        if (classes != null) {
            s1 += (" class=\"" + classes + "\"");
        }
        s1 += (">" + htmltext + "</a>");
        return s1;
    }

    public static String boxCSStoString(String[] values) {
        String result = null;

        boolean evenSame = values[1].equals(values[3]);
        boolean pairSame = evenSame && values[0].equals(values[2]);
        boolean allSame  = pairSame && values[0].equals(values[1]);

        if (allSame) {
            result = values[0];
        } else {
            int endJoin = pairSame ? 2 : (evenSame ? 3 : 4);
            result = StringUtils.join(values, ' ', 0, endJoin);
        }

        return result;
    }

}
