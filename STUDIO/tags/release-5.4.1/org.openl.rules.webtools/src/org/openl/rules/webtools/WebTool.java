package org.openl.rules.webtools;

import org.apache.commons.lang.ArrayUtils;
import org.openl.rules.table.ui.ICellFont;

import org.openl.util.RuntimeExceptionWrapper;
import org.openl.util.StringTool;

import java.util.Map;

import javax.servlet.ServletRequest;

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

    public static StringBuilder fontToHtml(ICellFont font, StringBuilder buf) {
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

        short[] color = font.getFontColor();

        buf.append("; color: " + toHexString(color));

        return buf;
    }

    static public String htmlStringWithSelections(String src, String[] tokens) {
        StringHighlighter sf = new StringHighlighter(tokens, src);        
        return sf.highlightStringsInText();
    }

    @SuppressWarnings("unchecked")
    public static String listRequestParams(ServletRequest request) {
        return listRequestParams(request.getParameterMap(), null);
    }

    @SuppressWarnings("unchecked")
    public static String listRequestParams(ServletRequest request, String[] exceptParams) {
        return listRequestParams(request.getParameterMap(), exceptParams);
    }

    public static String listRequestParams(Map<String, String[]> paramsMap, String[] exceptParams) {
        StringBuilder buf = new StringBuilder();

        for (String paramName : paramsMap.keySet()) {
            if (ArrayUtils.contains(exceptParams, paramName)) {
                continue;
            }
            if (buf.length() != 0) {
                buf.append('&');
            }
            String[] paramValues = paramsMap.get(paramName);
            buf.append(paramName).append('=').append(
                    StringTool.encodeURL(paramValues[0]));
        }

        return buf.toString();
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
