package org.openl.rules.webstudio;

import org.openl.rules.table.formatters.Formats;
import org.openl.rules.webstudio.web.Props;
import org.openl.rules.webstudio.web.admin.AdministrationSettings;

public final class WebStudioFormats implements Formats {
    private static final WebStudioFormats INSTANCE = new WebStudioFormats();

    private WebStudioFormats() {
    }

    public static WebStudioFormats getInstance() {
        return INSTANCE;
    }

    @Override
    public String date() {
        return Props.text(AdministrationSettings.DATE_PATTERN);
    }

    @Override
    public String dateTime() {
        return Props.text(AdministrationSettings.DATETIME_PATTERN);
    }
}
