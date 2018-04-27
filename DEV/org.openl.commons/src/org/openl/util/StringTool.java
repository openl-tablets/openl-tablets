package org.openl.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public class StringTool {

    public static final String NEW_LINE = "\n";

    public static StringBuilder append(StringBuilder buf, char c, int n) {
        for (int i = 0; i < n; i++) {
            buf.append(c);
        }
        return buf;
    }

    // TODO Move to URLUtils class
    public static String encodeURL(String url) {
        String encodedUrl = null;
        if (StringUtils.isBlank(url)) {
            return url;
        }
        try {
            encodedUrl = URLEncoder.encode(url, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encodedUrl;
    }

    // TODO Move to URLUtils class
    public static String decodeURL(String url) {
        String decodedUrl = null;
        if (StringUtils.isBlank(url)) {
            return url;
        }
        try {
            decodedUrl = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return decodedUrl;
    }

    public static int indexOfClosingBracket(String src, char openingBracket, char closingBracket, int fromIndex) {
        int len = src.length();
        int cnt = 1;
        for (int i = fromIndex; i < len; i++) {
            char c = src.charAt(i);

            if (c == closingBracket) {
                if (--cnt == 0) {
                    return i;
                }
            } else if (c == openingBracket) {
                ++cnt;
            }
        }
        return -1;
    }

    public static String lastToken(String src, String delim) {
        String[] tokens = tokenize(src, delim);
        return tokens.length > 0 ? tokens[tokens.length - 1] : "";
    }

    public static String makeJavaIdentifier(String src) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (i == 0) {
                buf.append(Character.isJavaIdentifierStart(c) ? c : '_');
            } else {
                buf.append(Character.isJavaIdentifierPart(c) ? c : '_');
            }
        }

        return buf.toString();
    }

    public static String[] openBrackets(String src, char openingBracket, char closingBracket, String ignore) {
        int len = src.length();
        List<String> v = new ArrayList<String>();

        for (int i = 0; i < len; i++) {
            char c = src.charAt(i);

            if (c == openingBracket) {
                int closed = indexOfClosingBracket(src, openingBracket, closingBracket, i + 1);
                if (closed == -1) {
                    throw new RuntimeException("Expected: " + closingBracket);
                }

                v.add(src.substring(i + 1, closed));
                i = closed;
                continue;
            }

            if (ignore.indexOf(c) == -1) {
                throw new RuntimeException("UnExpected: " + c);
            }

        }

        return v.toArray(new String[v.size()]);
    }

    public static String[] splitLines(String src) {
        BufferedReader br = new BufferedReader(new StringReader(src));
        List<String> v = new ArrayList<String>();
        String s;

        try {
            try {
                while ((s = br.readLine()) != null) {
                    v.add(s);
                }
                return v.toArray(new String[v.size()]);
            } finally {
                br.close();
            }
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }

    }

    public static String[] tokenize(String src, String delim) {
        StringTokenizer st = new StringTokenizer(src, delim);
        int cnt = st.countTokens();
        String[] res = new String[cnt];
        for (int i = 0; i < res.length; i++) {
            res[i] = st.nextToken();
        }
        return res;
    }

    static public String untab(String src, int tabSize) {
        StringBuilder buf = new StringBuilder(src.length() + 10);

        for (int i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c != '\t') {
                buf.append(c);
            } else {
                buf.append(' ');

                int extra = buf.length() % tabSize;
                if (extra != 0) {
                    append(buf, ' ', tabSize - extra);
                }
            }
        }
        return buf.toString();
    }

    static public String getFileNameOfJavaClass(Class<?> c) {
        return c.getName().replace('.', '/') + ".java";
    }

    /**
     * Split the string by the splitSymbol. To avoid splitting escapeSymbol
     * is used. Trims the splitted result. For examples see tests.
     * 
     * @param src source to process. Can`t be <code>null</code>.
     * @param splitSymbol the delimiting symbol. Can`t be <code>null</code>.
     * @param escapeSymbol the escaper, that is used to break splitting by
     *            splitSymbol. If <code>null</code>, the splitSymbol array
     *            will be returned.
     * @return the array of strings computed by splitting this string around
     *         matches of the given splitSymbol and escaped by escaper.
     */
    public static String[] splitAndEscape(String src, String splitSymbol, String escapeSymbol) {
        String[] result;
        String[] tokens = src.split(splitSymbol);
        List<String> resultList = new ArrayList<String>();
        StringBuilder buf = new StringBuilder();
        if (escapeSymbol != null) {
            for (int i = 0; i < tokens.length; i++) {
                if (tokens[i].endsWith(escapeSymbol)) {
                    String noEscapeToken = tokens[i].substring(0, tokens[i].length() - 1);
                    buf.append(noEscapeToken).append(splitSymbol);
                } else {
                    if (buf.length() == 0) {
                        tokens[i] = tokens[i].trim();
                        resultList.add(tokens[i]);
                    } else {
                        buf.append(tokens[i]);
                        resultList.add(buf.toString());
                        buf.delete(0, buf.length());
                    }
                }
            }
            result = resultList.toArray(new String[0]);
        } else {
            result = tokens;
        }

        return result;
    }

    public static String insertStringToString(String baseStr, String strToInsertBefore, String insertion) {
        String src = baseStr;
        String[] tokens = src.split(strToInsertBefore);
        StringBuilder strBuf = new StringBuilder();
        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            strBuf.append(token);
            if (!(i == tokens.length - 1)) {
                strBuf.append(insertion);
                strBuf.append(strToInsertBefore);
            }
        }
        return strBuf.toString();
    }
}
