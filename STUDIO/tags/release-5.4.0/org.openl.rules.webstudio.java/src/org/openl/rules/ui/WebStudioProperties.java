/**
 * Created Jan 24, 2007
 */
package org.openl.rules.ui;

/**
 * @author snshor
 *
 */
public class WebStudioProperties {

    static public final String STUDIO_MODE = "org.openl.rules.ui.WebStudio.mode", BUSINESS_MODE = "business",
            DEVELOPER_MODE = "developer";

    String mode = BUSINESS_MODE;

    String tableViewMode;

    public WebStudioProperties() {
    }

    public String getMode() {
        return mode;
    }

    public String getTableViewMode() {
        return tableViewMode == null ? mode : tableViewMode;
    }

    public void setMode(String mode) {
        this.mode = mode;
        tableViewMode = null;
    }

    public void setTableViewMode(String tableViewMode) {
        this.tableViewMode = tableViewMode;
    }

}
