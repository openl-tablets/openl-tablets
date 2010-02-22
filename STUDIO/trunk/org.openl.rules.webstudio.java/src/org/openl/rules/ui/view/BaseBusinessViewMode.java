package org.openl.rules.ui.view;

import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.ui.OpenLWrapperInfo;
import org.openl.util.StringTool;

public abstract class BaseBusinessViewMode extends WebStudioViewMode {

    public static final String TYPE = "business";

    @Override
    public String getDisplayName(OpenLWrapperInfo wrapper) {

        String displayName = wrapper.getDisplayName();

        if (displayName.equals(wrapper.getWrapperClassName())) {
            displayName = StringTool.lastToken(displayName, ".");
        }

        return displayName;
    }

    @Override
    public String getTableMode() {
        return IXlsTableNames.VIEW_BUSINESS;
    }

    @Override
    public Object getType() {
        return TYPE;
    }

    @Override
    public boolean select(TableSyntaxNode tsn) {

        String view = null;
        String name = null;
        if (!ITableNodeTypes.XLS_PROPERTIES.equals(tsn.getType())) {
            ITableProperties tableProperties = tsn.getTableProperties();
    
            if (tableProperties != null) {
                // FIXME: there is no such property 'view'!!
                // author: DLiauchuk
                view = tableProperties.getPropertyValueAsString("view");
                name = tableProperties.getName();
            }
        } else {
            name = tsn.getHeader().getSourceString();
        }

        return name != null && (view == null || view.indexOf(IXlsTableNames.VIEW_BUSINESS) >= 0);
    }

}