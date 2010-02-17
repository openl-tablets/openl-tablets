package org.openl.rules.ui.view;

import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.ui.OpenLWrapperInfo;
import org.openl.rules.ui.tree.BaseTableTreeNodeBuilder;
import org.openl.rules.ui.tree.OpenMethodInstancesGroupTreeNodeBuilder;
import org.openl.rules.ui.tree.TableInstanceTreeNodeBuilder;
import org.openl.rules.ui.tree.TableTreeNodeBuilder;
import org.openl.rules.ui.tree.TableVersionTreeNodeBuilder;
import org.openl.rules.ui.tree.TreeNodeBuilder;
import org.openl.rules.ui.tree.WorkbookTreeNodeBuilder;
import org.openl.rules.ui.tree.WorksheetTreeNodeBuilder;
import org.openl.util.StringTool;

public class DeveloperViewMode extends WebStudioViewMode {

    private static final BaseTableTreeNodeBuilder[][] sorters = {
            { new TableTreeNodeBuilder(), new OpenMethodInstancesGroupTreeNodeBuilder(),
                    new TableInstanceTreeNodeBuilder(), new TableVersionTreeNodeBuilder() },
            { new WorkbookTreeNodeBuilder(), new WorksheetTreeNodeBuilder(),
                    new OpenMethodInstancesGroupTreeNodeBuilder(), new TableInstanceTreeNodeBuilder(),
                    new TableVersionTreeNodeBuilder() } };

    private static final String[][] folders = { { "By Type", "Organize Project by component type", "" },
            { "By File", "Organize project by physical location" } };

    public DeveloperViewMode() {
        displayName = "Developer Mode. Provides all the technical details";
    }

    @Override
    public String getDisplayName(OpenLWrapperInfo wrapper) {

        String displayName = wrapper.getDisplayName();
        
        if (displayName.equals(wrapper.getWrapperClassName())) {
            displayName = StringTool.lastToken(displayName, ".");
        }
        return displayName + " (" + wrapper.getWrapperClassName() + ")";
    }

    @Override
    public String[][] getFolders() {
        return folders;
    }

    @Override
    public TreeNodeBuilder[][] getBuilders() {
        return sorters;
    }

    @Override
    public String getTableMode() {
        return IXlsTableNames.VIEW_DEVELOPER;
    }

    @Override
    public Object getType() {
        return WebStudioViewMode.DEVELOPER_MODE_TYPE;
    }

    @Override
    public boolean select(TableSyntaxNode tsn) {
        return true;
    }
}