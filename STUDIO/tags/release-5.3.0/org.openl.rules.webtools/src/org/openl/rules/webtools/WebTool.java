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
    
    /**
     * Finds the given array of strings in the text and highlight it with <b> tags 
     * for further displaying on UI. Finds detached words and parts of the words.
     *
     */
    static class StringHighlighter {
        
        private final String BOLD_OPEN = "<b>";
        private final String BOLD_CLOSE = "</b>";
        // tokens to be highlighted in the text
        private String[] tokensToHighlight;
        
        //last found token as it is in text
        private String currentToken;
        
        //text for highlighting
        private String text;
        
        //index of last found word
        private int lastSelected = -1;
        
        public StringHighlighter(String[] tokens, String src) {           
            this.tokensToHighlight = tokens;
            this.text = src;
        }

        private int findTokenFromPos(int startPos) {
            int textLength = text.length();
            lastSelected = -1;

            for (int i = tokensToHighlight.length - 1; i >= 0; i--) {                
                int idx = findToken(tokensToHighlight[i].toLowerCase(), startPos);
                if (idx >=0 && idx < textLength) {
                    lastSelected = i;
                    textLength = idx;                    
                }
            }

            if (lastSelected < 0) {
                return -1;
            }
            currentToken = text.substring(textLength, textLength + tokensToHighlight[lastSelected].length()); 
            return textLength;
        }
        
        /**
         * 
         * Searches the start index of a token in the text from the given position.
         * If no such token exists in the text, then -1 is returned.
         * @param token Token to be found.
         * @param startPos Position in the text to start looking for a token.
         * @return Index in the text where token starts.
         */
        private int findToken(String token, int startPos) {
            String text = this.text;
            int tokenIndex = 0;
            tokenIndex = text.toLowerCase().indexOf(token, startPos);                
            return tokenIndex;            
        }
        
        /**
         * Highlight strings in the text. Find  
         * detached words and parts of words.
         * @return Highlighted strings in text. For further use on UI.
         */
        public String highlightStringsInText() {
            StringBuffer buf = new StringBuffer();
            int startPos = 0;
            for (;;) {
                int nextStart = findTokenFromPos(startPos);
                if (nextStart < 0) {
                    buf.append(text.substring(startPos));
                    return buf.toString();
                }
                buf.append(text.substring(startPos, nextStart));
                buf.append(BOLD_OPEN);
                buf.append(currentToken);
                buf.append(BOLD_CLOSE);
                startPos = nextStart + tokensToHighlight[lastSelected].length();
            }
        }
    }

    // static public String makeXlsUrl()
    // {
    // return "&wbPath=" + parser.wbPath + "&wbName=" + parser.wbName
    // + "&wsName=" + parser.wsName + "&range=" + parser.range;
    //
    // }
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

        // !!! IColorFilter fontFilter = getColorFilter(FONT_COLOR_FILTER_IDX);

        // !!! short[] color = fontFilter == null ? font.getFontColor() :
        // fontFilter.filterColor(font.getFontColor());
        short[] color = font.getFontColor();

        buf.append("; color: " + toHexString(color));

        return buf;
    }

    static public String htmlStringWithSelections(String src, String[] tokens) {
        StringHighlighter sf = new StringHighlighter(tokens, src);        
        return sf.highlightStringsInText();
    }

    public static String listParamsExcept(String[] usedParams, HttpServletRequest request) {
        return listParamsExcept(usedParams, request.getParameterMap());
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

                // buf.append('?');
            } else {
                buf.append('&');
            }

            String[] values = pmap.get(pname);
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

                // buf.append('?');
            } else {
                buf.append('&');
            }

            String value = (String) pmap.get(pname);
            buf.append(pname).append('=').append(StringTool.encodeURL(value));
        }

        return (buf == null) ? "" : buf.toString();
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

        if (parser.isExcelFile()) {
            String ret = "" + "&wbPath=" + parser.wbPath + "&wbName=" + parser.wbName + "&wsName=" + parser.wsName
                    + "&range=" + parser.range;

            return ret;
        }

        WordUrlParser wdparser = new WordUrlParser();
        try {
            wdparser.parse(url);
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }

        String ret = "" + "&wdPath=" + wdparser.wdPath + "&wdName=" + wdparser.wdName + "&wdParStart="
                + wdparser.wdParStart + "&wdParEnd=" + wdparser.wdParEnd;

        return ret;
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

    static public String toHexString(short[] x) {
        if (x == null) {
            return "#000000";
        }
        return "#" + toHex(x[0]) + toHex(x[1]) + toHex(x[2]);
    }

    /**
     * Returns 'rgfb(red,green,blue)' string
     *
     * @param color
     *
     * @return 'rgb(red,green,blue)' string
     */
    static public String toRgbString(short[] color) {
        return "rgb(" + String.valueOf(color[0]) + "," + String.valueOf(color[1]) + "," + String.valueOf(color[2])
                + ")";
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
}
