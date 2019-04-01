package org.openl.commons.web.jsf.facelets.fn;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringEscapeUtils;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.commons.web.util.WebTool;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;

import com.sdicons.json.mapper.JSONMapper;
import com.sdicons.json.mapper.MapperException;

/**
 * JSF functions.
 *
 * @author Andrey Naumenko
 */
public final class JSFFunctions {
    private JSFFunctions() {
    }

    /**
     * Concatenates the specified string <code>str2</code> to the end of this string <code>str1</code>.
     *
     * @param str1 the first string
     * @param str2 the second string
     *
     * @return concatenation result
     */
    public static String concat(String str1, String str2) {
        return str1 + str2;
    }

    public static String joinAll(Object[] values, String separator) {
        return StringUtils.join(values, separator);
    }

    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified key.
     *
     * @param map Map
     * @param key key value
     *
     * @return <tt>true</tt> if this map contains a mapping for the specified key.
     */
    public static boolean contains(Map<?, ?> map, Object key) {
        return (map != null) && map.containsKey(key);
    }

    public static boolean hasGlobalMessages() {
        return FacesUtils.getFacesContext().getMessages(null).hasNext();
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

    private static String formatCurrentDate(String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);

        return dateFormat.format(new Date());
    }

    public static boolean isLocalRequest() {
        return WebTool.isLocalRequest(FacesUtils.getRequest());
    }

    /**
     * Get map entry set.
     *
     * @param map input map
     *
     * @return entry set
     */
    public static Set<?> mapEntrySet(Map<?, ?> map) {
        return (map != null) ? map.entrySet() : Collections.EMPTY_SET;
    }

    public static String unescape(String string) {
        return StringEscapeUtils.unescapeHtml4(string);
    }

    public static String encodeURL(String url) {
        return StringTool.encodeURL(url);
    }

    public static String getSystemProp(String name) {
        return System.getProperty(name);
    }

    public static boolean isSystemProp(String name) {
        return getSystemProp(name) != null;
    }

    public static String toJSON(Object value) throws MapperException {
        return JSONMapper.toJSON(value).render(true);
    }

    public static String escapeJS(String value) {
        return StringEscapeUtils.escapeEcmaScript(value);
    }

    public static String toJSText(String str) {
        return str == null ? null : str.replace("\\", "\\\\").replace("\"", "\\\"").replace("\'", "\\\'");
    }
}
