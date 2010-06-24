package org.openl.rules.ui.view;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.project.model.Module;
import org.openl.util.StringTool;

public abstract class BaseDeveloperViewMode extends WebStudioViewMode {

    public static final String TYPE = "developer";

    @Override
    public String getDisplayName(Module module) {

        String displayName = module.getName();
        
        if (displayName.equals(module.getClassname())) {
            displayName = StringTool.lastToken(displayName, ".");
        }
        return displayName + " (" + module.getClassname() + ")";
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