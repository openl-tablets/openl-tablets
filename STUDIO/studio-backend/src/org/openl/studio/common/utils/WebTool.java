package org.openl.studio.common.utils;

import org.openl.util.StringTool;

/**
 * DOCUMENT ME!
 *
 * @author Stanislav Shor
 */
public final class WebTool {
    private WebTool() {
    }

    public static String getContentDispositionValue(String fileName) {
        String encodedFileName = StringTool.encodeURL(fileName);
        return "attachment; filename=" + encodedFileName + "; filename*=UTF-8''" + encodedFileName;
    }

}
