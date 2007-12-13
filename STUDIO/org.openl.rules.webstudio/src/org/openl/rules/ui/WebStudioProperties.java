package org.openl.rules.ui;

/**
 * DOCUMENT ME!
 *
 * @author Stanislav Shor
 */
public class WebStudioProperties {
    public static final String STUDIO_MODE = "org.openl.rules.ui.WebStudio.mode";
    public static final String BUSINESS_MODE = "business";
    public static final String DEVELOPER_MODE = "developer";
    String mode = BUSINESS_MODE;
    String tableViewMode;

    public WebStudioProperties() {}

    public String getMode() {
        return this.mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
        this.tableViewMode = null;
    }

    public String getTableViewMode() {
        return (this.tableViewMode == null) ? this.mode : this.tableViewMode;
    }

    public void setTableViewMode(String tableViewMode) {
        this.tableViewMode = tableViewMode;
    }
}
