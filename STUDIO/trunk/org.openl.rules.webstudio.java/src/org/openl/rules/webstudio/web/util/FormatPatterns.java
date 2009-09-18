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
    
    /**
     * Return date and time pattern.
     * @return
     */
    public String getDateTimePattern() {
        return "MM/dd/yyyy HH:mm";
    }
    
    /**
     * Return GMT TimeZone.
     * @return
     */
    public TimeZone getGMTTimeZone() {
        return TimeZone.getTimeZone("GMT");
    }
    
    /**
     * Gets the default TimeZone for this host. The source of the default TimeZone may vary with implementation. 
     * @return
     */
    public TimeZone getDefaultTimeZone() {
        return TimeZone.getDefault();
    }
}
