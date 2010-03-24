package org.openl.rules.ui.view;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.OpenLWrapperInfo;
import org.openl.util.StringTool;

public abstract class BaseDeveloperViewMode extends WebStudioViewMode {

    public static final String TYPE = "developer";

    @Override
    public String getDisplayName(OpenLWrapperInfo wrapper) {

        String displayName = wrapper.getDisplayName();
        
        if (displayName.equals(wrapper.getWrapperClassName())) {
            displayName = StringTool.lastToken(displayName, ".");
        }
        return displayName + " (" + wrapper.getWrapperClassName() + ")";
    }

    @Override
    public String getTableMode() {
        return IXlsTableNames.VIEW_DEVELOPER;
    }

    @Override
    public Object getType() {
        return TYPE;
    }

    @Override
    public boolean select(TableSyntaxNode tsn) {
        return true;
    }

}