package org.openl.util;

import java.text.MessageFormat;

/**
 *
 * @author Aleh Bykhavets
 */
public class MsgHelper {
    public static String format(String pattern, Object... params) {
        return MessageFormat.format(pattern, params);
    }
}
