package org.openl.rules.webstudio.web;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.openl.rules.webstudio.WebStudioFormats;
import org.openl.rules.webstudio.web.admin.ProjectsInHistoryController;

public class ProjectHistoryItem {

    private final String version;
    private final String modifiedOn;
    private final boolean current;

    public ProjectHistoryItem(String version) {
        current = version.endsWith(ProjectsInHistoryController.CURRENT_VERSION);
        this.version = (version.replaceAll(ProjectsInHistoryController.CURRENT_VERSION + "$", ""));
        SimpleDateFormat formatter = new SimpleDateFormat(WebStudioFormats.getInstance().dateTime());
        String format;
        try {
            long time = Long.parseLong(this.version );
            format = formatter.format(new Date(time));
        } catch (NumberFormatException e) {
            format = this.version;
        }
        this.modifiedOn = format;
    }

    public String getVersion() {
        return version;
    }

    public String getModifiedOn() {
        return modifiedOn;
    }

    public boolean isCurrent() {
        return current;
    }
}
