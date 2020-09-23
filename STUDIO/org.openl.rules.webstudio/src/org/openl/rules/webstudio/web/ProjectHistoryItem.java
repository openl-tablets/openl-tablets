package org.openl.rules.webstudio.web;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import org.openl.rules.ui.FileBasedProjectHistoryManager;
import org.openl.rules.webstudio.WebStudioFormats;

public class ProjectHistoryItem {

    private final String version;
    private final String modifiedOn;

    public ProjectHistoryItem(String version) {
        this.version = version;
        SimpleDateFormat formatter = new SimpleDateFormat(WebStudioFormats.getInstance().dateTime());
        String format;
        try {
            long time = Long.parseLong(version.replaceAll(Pattern.quote(FileBasedProjectHistoryManager.CURRENT_VERSION)+ "$",""));
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
