package org.openl.rules.lang.xls.syntax;

import org.apache.commons.codec.digest.DigestUtils;
import org.openl.util.StringTool;

/**
 * Created by Andrei Ostrovski on 14.06.14.
 */
public class TableUtils {

    public static String makeTableId(String uri) {
        String decodedUri = StringTool.decodeURL(uri);
        return DigestUtils.md5Hex(decodedUri);
    }

}
