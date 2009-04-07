/*
 * Created on 24.12.2004
 */
package org.openl.eclipse.util;

import java.io.File;
import java.net.URL;

import org.openl.util.RuntimeExceptionWrapper;

/**
 * @author smesh
 */
public class UrlUtil {

    /**
     * URL protocol: "file:"
     */
    static public final String FILE_PROTOCOL = "file:";

    /**
     * Guesses if path is File URL.
     *
     * @return canonical URL for the File or null.
     */
    static private URL guessFileURL(String url) {
        url = ltrimFileURL(url);

        // Wintel's "C:..." are files, other - not (poor Mac:)
        int idx = url.indexOf(':');
        if (idx == 0 || idx > 1) {
            return null;
        }

        try {
            return new File(url).getCanonicalFile().toURL();
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    static private String ltrimFileURL(String url) {
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

    /**
     * Returns canonical URL for the path.
     *
     * Note: File URLs are without "file:" protocol.
     */
    static public String toCanonicalUrl(String path) {
        String url = toUrl(path).toExternalForm();

        url = ltrimFileURL(url);

        return url;
    }

    /**
     * Returns canonical URL for the (parent,child)-resource.
     */
    public static String toCanonicalUrl(String parent, String child) {
        String url = toCanonicalUrl(parent);
        if (child != null && child.length() > 0) {
            url += '/' + child;
        }
        return url;
    }

    /**
     * Returns URL for the path.
     */
    static public URL toUrl(String path) {
        // 'new URL()' is NOGOOD for Wintel's names - guess ourself.
        URL url = guessFileURL(path);
        if (url != null) {
            return url;
        }

        try {
            return new URL(path);
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    static public URL[] toUrl(String[] path) {
        URL[] result = new URL[path.length];
        for (int i = 0; i < path.length; i++) {
            result[i] = toUrl(path[i]);
        }
        return result;
    }

}