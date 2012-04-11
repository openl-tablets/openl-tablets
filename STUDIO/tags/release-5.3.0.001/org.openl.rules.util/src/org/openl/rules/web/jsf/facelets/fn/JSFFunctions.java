package org.openl.rules.web.jsf.facelets.fn;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.faces.context.FacesContext;
import javax.servlet.ServletRequest;

import org.apache.commons.lang.StringEscapeUtils;
import org.openl.rules.util.net.NetUtils;

/**
 * JSF functions.
 *
 * @author Andrey Naumenko
 */
public class JSFFunctions {
    /**
     * Concatenates the specified string <code>str2</code> to the end of this
     * string <code>str1</code>.
     *
     * @param str1 the first string
     * @param str2 the second string
     *
     * @return concatenation result
     */
    public static String concat(String str1, String str2) {
        return str1 + str2;
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.
     *
     * @param map Map
     * @param key key value
     *
     * @return <tt>true</tt> if this map contains a mapping for the specified
     *         key.
     */
    public static boolean contains(Map map, Object key) {
        return (map != null) && map.containsKey(key);
    }

    public static Date currentDate() {
        return new Date();
    }

    public static String currentHour() {
        return formatCurrentDate("kk");
    }

    public static String currentMinute() {
        return formatCurrentDate("mm");
    }

    public static String currentYear() {
        return formatCurrentDate("yyyy");
    }

    public static String escapeJavaScript(String string) {
        return StringEscapeUtils.escapeJavaScript(string);
    }

    private static String formatCurrentDate(String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);

        return dateFormat.format(new Date());
    }

    public static boolean isLocalRequest() {
        return NetUtils.isLocalRequest((ServletRequest) FacesContext.getCurrentInstance().getExternalContext()
                .getRequest());
    }

    /**
     * Get map entry set.
     *
     * @param map input map
     *
     * @return entry set
     */
    public static Set mapEntrySet(Map map) {
        return (map != null) ? map.entrySet() : Collections.EMPTY_SET;
    }

    public static String unescape(String string) {
        return StringEscapeUtils.unescapeHtml(string);
    }
}
