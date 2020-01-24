package org.openl.commons.web.jsf.facelets.fn;

import org.apache.commons.lang3.StringEscapeUtils;
import org.openl.util.StringTool;

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

    public static String encodeURL(String url) {
        return StringTool.encodeURL(url);
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
