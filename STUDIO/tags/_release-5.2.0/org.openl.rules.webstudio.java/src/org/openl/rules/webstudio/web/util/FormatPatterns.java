package org.openl.rules.webstudio.web.util;

import java.util.TimeZone;


/**
 * Contains default format patterns.
 *
 * @author Andrey Naumenko
 */
public class FormatPatterns {
    /**
     * Return date pattern.
     *
     * @return
     */
    public String getDatePattern() {
        return "MM/dd/yyyy";
    }

    public TimeZone getTimeZone() {
        return TimeZone.getTimeZone("GMT");
    }
}
