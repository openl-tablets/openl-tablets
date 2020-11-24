package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.TableSyntaxNodeUtils;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.ui.IProjectTypes;

/**
 * Build tree node for one version of some table.
 *
 * @author PUdalau
 */
public class TableVersionTreeNodeBuilder extends BaseTableTreeNodeBuilder {

    private static final String VERSION_NOT_SPECIFIED = "Version not specified";

    @Override
    public String[] getDisplayValue(Object nodeObject, int i) {
        TableSyntaxNode tsn = (TableSyntaxNode) nodeObject;
        ITableProperties tableProperties = tsn.getTableProperties();

        String name = null;
        String display = null;
        String version = null;

        if (tableProperties != null) {
            name = tableProperties.getName();
            display = name;
            version = tableProperties.getVersion();
        }

        if (name == null) {
            name = TableSyntaxNodeUtils.str2name(tsn.getGridTable().getCell(0, 0).getStringValue(), tsn.getNodeType());
            display = tsn.getGridTable().getCell(0, 0).getStringValue();
        }
        if (version == null) {
            version = VERSION_NOT_SPECIFIED;
        }

        String sfx = " [" + version + "]";

        return new String[] { name + sfx, display + sfx, display + sfx };
    }

    @Override
    public String getType(Object nodeObject) {
        TableSyntaxNode tsn = (TableSyntaxNode) nodeObject;
        return String.format("%s.%s", IProjectTypes.PT_TABLE, tsn.getType()).intern();
    }

    @Override
    public String getUrl(Object nodeObject) {
        TableSyntaxNode tsn = (TableSyntaxNode) nodeObject;
        return tsn.getUri();
    }
}
