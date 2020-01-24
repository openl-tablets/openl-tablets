package org.openl.rules.webstudio.util;

import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.util.StringTool;

/**
 * DOCUMENT ME!
 *
 * @author Stanislav Shor
 */
public final class WebTool {
    private WebTool() {
    }

    public static String listRequestParams(ServletRequest request, String[] exceptParams) {
        StringBuilder buf = new StringBuilder();

        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            String paramName = entry.getKey();
            if (ArrayUtils.contains(exceptParams, paramName)) {
                continue;
            }
            if (buf.length() != 0) {
                buf.append('&');
            }
            String[] paramValues = entry.getValue();
            buf.append(paramName).append('=').append(StringTool.encodeURL(paramValues[0]));
        }

        return buf.toString();
    }

    public static String getContentDispositionValue(String fileName) {
        String encodedFileName = StringTool.encodeURL(fileName);
        return "attachment; filename=" + encodedFileName + "; filename*=UTF-8''" + encodedFileName;
    }

}
