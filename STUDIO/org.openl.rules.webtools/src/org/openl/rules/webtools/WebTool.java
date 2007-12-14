package org.openl.rules.webtools;

import org.openl.rules.table.ui.ICellFont;

import org.openl.util.ArrayTool;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;

import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;


/**
 * DOCUMENT ME!
 *
 * @author Stanislav Shor
 */
public class WebTool extends StringTool {
    static public String htmlStringWithSelections(String src, String[] tokens) {
        StringFinder sf = new StringFinder();
        sf.tokens = tokens;
        sf.src = src;
        return sf.makeSelections();
    }

    /**
     * DOCUMENT ME!
     *
     * @param url
     * @param title
     * @param htmltext - pre-formatted html text
     * @param target
     *
     * @return
     */
    public static String urlLink(String url, String title, String htmltext, String target) {
        String s1 = "<a href=\"" + url + "\"";
        if (title != null) {
            s1 += (" title=\"" + title + "\"");
        }
        if (target != null) {
            s1 += (" target=\"" + target + "\"");
        }
        s1 += (">" + htmltext + "</a>");
        return s1;
    }

//	static public String makeXlsUrl()
//	{
//		return "&wbPath=" + parser.wbPath + "&wbName=" + parser.wbName
//		+ "&wsName=" + parser.wsName + "&range=" + parser.range;
//
//	}
    static public StringBuffer fontToHtml(ICellFont font, StringBuffer buf) {
        if (font == null) {
            return buf;
        }

        if (font.isUnderlined()) {
            buf.append("text-decoration: underline;");
        }

        buf.append("font-family: ").append(font.getName());
        buf.append("; font-size: ").append(font.getSize() + 2);
        if (font.isItalic()) {
            buf.append("; font-style: italic");
        }
        if (font.isBold()) {
            buf.append("; font-weight: bold");
        }

//!!!		IColorFilter fontFilter = getColorFilter(FONT_COLOR_FILTER_IDX);

//!!!		short[] color = fontFilter == null ? font.getFontColor() : fontFilter.filterColor(font.getFontColor());
        short[] color = font.getFontColor();

        buf.append("; color: " + toHexString(color));

        return buf;
    }

    static public String toHexString(short[] x) {
        if (x == null) {
            return "#000000";
        }
        return "#" + toHex(x[0]) + toHex(x[1]) + toHex(x[2]);
    }

    static public String toHex(short x) {
        String s = Integer.toHexString(x);

        switch (s.length()) {
            case 1:
                return "0" + s;
            case 2:
                return s;
        }
        return s.substring(s.length() - 2);
    }

    static public String makeXlsOrDocUrl(String url) {
        if (url == null) {
            return "#";
        }

        XlsUrlParser parser = new XlsUrlParser();
        try {
            parser.parse(url);
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }

        if (parser.wbName.endsWith(".xls")) {
            String ret = "" + "&wbPath=" + parser.wbPath + "&wbName=" + parser.wbName
                + "&wsName=" + parser.wsName + "&range=" + parser.range;

            return ret;
        }

        WordUrlParser wdparser = new WordUrlParser();
        try {
            wdparser.parse(url);
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }

        String ret = "" + "&wdPath=" + wdparser.wdPath + "&wdName=" + wdparser.wdName
            + "&wdParStart=" + wdparser.wdParStart + "&wdParEnd=" + wdparser.wdParEnd;

        return ret;
    }

    public static String listParamsExcept(String[] usedParams, Map<String, String[]> pmap) {
        StringBuffer buf = null;

        for (Iterator iter = pmap.keySet().iterator(); iter.hasNext();) {
            String pname = (String) iter.next();

            if (ArrayTool.contains(usedParams, pname)) {
                continue;
            }
            if (buf == null) {
                buf = new StringBuffer(100);

//				buf.append('?');
            } else {
                buf.append('&');
            }

            String[] values = (String[]) pmap.get(pname);
            buf.append(pname).append('=').append(StringTool.encodeURL(values[0]));
        }

        return (buf == null) ? "" : buf.toString();
    }

    public static String listParamsExcept2(String[] usedParams, Map pmap) {
        StringBuffer buf = null;

        for (Iterator iter = pmap.keySet().iterator(); iter.hasNext();) {
            String pname = (String) iter.next();

            if (ArrayTool.contains(usedParams, pname)) {
                continue;
            }
            if (buf == null) {
                buf = new StringBuffer(100);

//				buf.append('?');
            } else {
                buf.append('&');
            }

            String value = (String) pmap.get(pname);
            buf.append(pname).append('=').append(StringTool.encodeURL(value));
        }

        return (buf == null) ? "" : buf.toString();
    }

    public static String listParamsExcept(String[] usedParams, HttpServletRequest request) {
        return listParamsExcept(usedParams, request.getParameterMap());
    }

    /**
     * Returns 'rgfb(red,green,blue)' string
     *
     * @param color
     *
     * @return 'rgb(red,green,blue)' string
     */
    static public String toRgbString(short[] color) {
        return "rgb(" + String.valueOf(color[0]) + "," + String.valueOf(color[1]) + ","
        + String.valueOf(color[2]) + ")";
    }

    static class StringFinder {
        int start = 0;
        String[] tokens;
        String src;
        int lastSelected = -1;

        String makeSelections() {
            StringBuffer buf = new StringBuffer();

            for (;;) {
                int nextStart = findFirst();
                if (nextStart < 0) {
                    buf.append(src.substring(start));
                    return buf.toString();
                }
                buf.append(src.substring(start, nextStart));
                buf.append("<b>");
                buf.append(tokens[lastSelected]);
                buf.append("</b>");
                start = nextStart + tokens[lastSelected].length();
            }
        }

        int findFirst() {
            int min = src.length();
            lastSelected = -1;

            for (int i = tokens.length - 1; i >= 0; i--) {
                int idx = findToken(tokens[i]);
                if (idx < 0) {
                    continue;
                }

                if (idx < min) {
                    lastSelected = i;
                    min = idx;
                }
            }

            if (lastSelected < 0) {
                return -1;
            }
            return min;
        }

        int findToken(String token) {
            int sx = start;
            int tlen = token.length();
            while (true) {
                int idx = src.indexOf(token, sx);
                if (idx < 0) {
                    return idx;
                }
                if (((idx > 0) && Character.isLetterOrDigit(src.charAt(idx - 1)))
                        || (((idx + tlen) < src.length())
                        && Character.isLetterOrDigit(src.charAt(idx + tlen)))) {
                    sx = idx + tlen;
                    continue;
                }
                return idx;
            }
        }
    }
}
