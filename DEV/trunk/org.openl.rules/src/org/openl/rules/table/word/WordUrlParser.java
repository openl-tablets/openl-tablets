/*
 * Created on Dec 23, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.table.word;

import java.io.File;
import java.net.URL;
import java.util.Map;

import org.openl.exception.OpenLRuntimeException;
import org.openl.main.SourceCodeURLTool;
import org.openl.rules.table.syntax.XlsURLConstants;

/**
 *
 * @author sam
 */

public class WordUrlParser implements XlsURLConstants {

    public static final String FILE_PROTOCOL = "file:";

    public String wdPath;
    public String wdName;
    public String wdParStart;

    public String wdParEnd;

    static boolean endsWithSlash(String s) {
        return s.length() > 0 && isSlash(s.charAt(s.length() - 1));
    }

    static boolean isSlash(char c) {
        return c == '/' || c == '\\';
    }

    /**
     * Guesses if path is File URL.
     *
     * @return canonical URL for the File or null.
     */
    URL guessFileURL(String url) {
        url = ltrimFileURL(url);

        // Wintel's "C:..." are files, other - not (poor Mac:)
        int idx = url.indexOf(':');
        if (idx == 0 || idx > 1) {
            return null;
        }

        try {
            return new File(url).getCanonicalFile().toURL();
        } catch (Throwable t) {
            throw new OpenLRuntimeException(t);
        }
    }

    String ltrimFileURL(String url) {
        url = url.replace('\\', '/');

        if (url.startsWith(FILE_PROTOCOL)) {
            url = url.substring(FILE_PROTOCOL.length());
        }

        // Wintel's "/C:..." - just in case...
        if (url.indexOf('/') == 0 && url.indexOf(':') == 2) {
            url = url.substring(1);
        }

        return url;
    }

    public void parse(String url) throws Exception {
        // String SCRIPT_NAME = "LaunchExcel.vbs";

        // scriptName =
        // toCanonicalUrl(OpenlRulesPlugin.getDefault().getBundle(),
        // SCRIPT_NAME);

        Map<String, String> urlMap = SourceCodeURLTool.parseUrl(url);

        String file = urlMap.get(FILE);

        wdParStart = urlMap.get(PARAGRAPH_NUM);
        if (wdParStart == null) {
            wdParStart = urlMap.get(PARAGRAPH_START);
        }
        wdParEnd = urlMap.get(PARAGRAPH_END);

        File f = new File(file).getCanonicalFile();

        wdPath = f.getParent();
        wdName = f.getName();

        // wsName = url.substring(iPound + 1, iAt);

        // range = url.substring(iAt + 1);

    }

    // public String toCanonicalUrl(Bundle pd, String path) throws IOException {
    // URL url = Platform.find(pd, new Path(path));
    // url = Platform.asLocalURL(url);
    // if (url == null) {
    // return null;
    // }
    // String s = toCanonicalURL(url.toString());
    //
    // // remove ending slash added by conversion
    // if (!endsWithSlash(path) && endsWithSlash(s))
    // s = s.substring(0, s.length() - 1);
    //
    // return s;
    // }

    /**
     * Returns canonical URL for the path. Note: File URLs are without "file:"
     * protocol.
     */
    public String toCanonicalURL(String path) {
        String url = toURL(path).toExternalForm();

        url = ltrimFileURL(url);

        return url;
    }

    /**
     * Returns canonical URL for the (parent,child)-resource.
     */
    public String toCanonicalURL(String parent, String child) {
        String url = toCanonicalURL(parent);
        if (child != null && child.length() > 0) {
            url += '/' + child;
        }
        return url;
    }

    /**
     * Returns URL for the path.
     */
    public URL toURL(String path) {
        // 'new URL()' is NOGOOD for Wintel's names - guess ourself.
        URL url = guessFileURL(path);
        if (url != null) {
            return url;
        }

        try {
            return new URL(path);
        } catch (Throwable t) {
            throw new OpenLRuntimeException(t);
        }
    }

    public URL[] toURL(String[] path) {
        URL[] urls = new URL[path.length];

        for (int i = 0; i < path.length; i++) {
            urls[i] = toURL(path[i]);
        }

        return urls;
    }

}
