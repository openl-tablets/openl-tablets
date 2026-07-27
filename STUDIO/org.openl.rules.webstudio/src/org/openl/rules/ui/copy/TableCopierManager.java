package org.openl.rules.ui.copy;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import org.openl.rules.table.IOpenLTable;
import org.openl.rules.ui.Message;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.StringUtils;

@Service("tableCopierManager")
@SessionScope
public class TableCopierManager {

    enum CopyType {
        CHANGE_NAMES,
        CHANGE_DIMENSION,
        CHANGE_VERSION
    }

    private String tableUri;
    private CopyType copyType;
    private TableCopier copier;

    public TableCopier getCopier() {
        return copier;
    }

    public IOpenLTable getTable() {
        return WebStudioUtils.getOrCreateWebStudio().getModel().getTable(tableUri);
    }

    public String getCopyType() {
        return copyType.name();
    }

    public void setCopyType(String copyType) {
        this.copyType = CopyType.valueOf(copyType);
        selectCopyType();
    }

    private void selectCopyType() {
        tableUri = null;
        init();
        copier = switch (copyType) {
            case CHANGE_NAMES -> new TableNamesCopier(getTable());
            case CHANGE_VERSION -> new VersionPropertyTableCopier(getTable());
            case CHANGE_DIMENSION -> new DimensionalPropertiesTableCopier(getTable());
        };
    }

    public String start() {
        copyType = CopyType.CHANGE_NAMES;
        selectCopyType();
        return null;
    }

    private void init() {
        String id = WebStudioUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);

        WebStudio studio = WebStudioUtils.getWebStudio();

        if (!StringUtils.isBlank(id)) {
            IOpenLTable table = studio.getModel().getTableById(id);
            if (table == null) {
                throw new Message("Table with id " + id + " does not exists");
            }
            tableUri = table.getUri();
        } else {
            tableUri = studio.getTableUri();
        }
    }

}
