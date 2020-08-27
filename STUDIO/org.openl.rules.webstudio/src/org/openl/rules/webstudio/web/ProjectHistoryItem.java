package org.openl.rules.webstudio.web;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openl.rules.webstudio.WebStudioFormats;

public class ProjectHistoryItem {

    private final String version;
    private final String modifiedOn;

    public ProjectHistoryItem(String version) {
        this.version = version;
        SimpleDateFormat formatter = new SimpleDateFormat(WebStudioFormats.getInstance().dateTime());
        String format;
        try {
            long time = Long.parseLong(version);
            format = formatter.format(new Date(time));
        } catch (NumberFormatException e) {
            format = version;
        }
        this.modifiedOn = format;
    }

    public String getVersion() {
        return version;
    }

    public String getModifiedOn() {
        return modifiedOn;
    }
}
