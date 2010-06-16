/**
 * Created Feb 3, 2007
 */
package org.openl.rules.ui;

import org.openl.rules.table.IGridTable;
import org.openl.rules.tableeditor.model.ui.util.HTMLHelper;
import org.openl.rules.ui.search.FileIndexer;

/**
 * @author snshor
 *
 */
public class TableInfo {

    private IGridTable table;
    private String displayName;
    private boolean runnable;
    private String uri;

    public TableInfo(IGridTable table, String displayName, boolean runnable) {
        this.table = table;
        this.displayName = displayName;
        this.runnable = runnable;
    }

    public TableInfo(IGridTable table, String displayName, boolean runnable, String uri) {
        this.table = table;
        this.displayName = displayName;
        this.runnable = runnable;
        this.uri = uri;
    }

    public String getDisplayName() {
        return displayName;
    }

    public IGridTable getTable() {
        return table;
    }

    public String getText() {
        return FileIndexer.showElementHeader(getUri());
    }

    public String getUri() {
        return table.getUri();
    }

    public String getUrl() {
        return HTMLHelper.makeXlsOrDocUrl(uri == null ? table.getUri() : uri);
    }

    public boolean isRunnable() {
        return runnable;
    }

}
