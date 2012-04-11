/*
 * Created on Dec 23, 2003
 *
 * Developed by OpenRules Inc. 2003
 */

package org.openl.rules.table.xls;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.openl.main.SourceCodeURLTool;
import org.openl.rules.table.syntax.XlsURLConstants;
import org.openl.util.FileTypeHelper;
import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author sam
 */

public class XlsUrlParser implements XlsURLConstants {

    public static final String FILE_PROTOCOL = "file:";

    public String wbPath;
    public String wbName;
    public String wsName;

    public String range;

    public String cell;

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
            throw RuntimeExceptionWrapper.wrap(t);
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

    public boolean isExcelFile() {
	    return FileTypeHelper.isExcelFile(wbName);
	}

    public void parse(String url) {
        // String SCRIPT_NAME = "LaunchExcel.vbs";

        // scriptName =
        // toCanonicalUrl(OpenlRulesPlugin.getDefault().getBundle(),
        // SCRIPT_NAME);

        Map<String, String> urlMap = SourceCodeURLTool.parseUrl(url);

        String file = urlMap.get(FILE);
        wsName = urlMap.get(SHEET);

        range = urlMap.get(RANGE);

        if (range == null) {
            // TODO line, col
            range = urlMap.get(CELL);
        }

        cell = urlMap.get(CELL);

        File f = null;
        try {
            f = new File(file).getCanonicalFile();
            wbPath = f.getParent();
            wbName = f.getName();
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        } catch (NullPointerException ex) {
            // there is no file representation
            // FIXME tempory hack to support generated dispatch tables
            wbPath = "/unexistingPath/";
            wbName = "unexistingSourceFile.xls";
        }

        

        // wsName = url.substring(iPound + 1, iAt);

        // range = url.substring(iAt + 1);

    }

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
            throw RuntimeExceptionWrapper.wrap(t);
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
