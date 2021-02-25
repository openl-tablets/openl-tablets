package org.openl.rules.lang.xls.syntax;

import org.apache.commons.codec.digest.DigestUtils;
import org.openl.util.StringTool;

/**
 * Created by Andrei Ostrovski on 14.06.14.
 */
public final class TableUtils {

    private TableUtils() {
    }

    public static String makeTableId(String uri) {
        if (uri == null) {
            return null;
        }
        String decodedUri = StringTool.decodeURL(uri);
        if (decodedUri != null) {
            decodedUri = decodedUri.substring(0, decodedUri.lastIndexOf(":"));
        }
        return DigestUtils.md5Hex(decodedUri);
    }

}
