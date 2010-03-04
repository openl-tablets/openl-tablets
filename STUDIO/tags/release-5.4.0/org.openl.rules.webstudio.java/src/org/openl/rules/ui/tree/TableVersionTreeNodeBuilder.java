package org.openl.rules.ui.tree;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.ui.IProjectTypes;

/**
 * Build tree node for one version of some table.
 * 
 * @author PUdalau
 */
public class TableVersionTreeNodeBuilder extends BaseTableTreeNodeBuilder {

    private static final String TABLE_VERSION = "Table Version";
    private static final String VERSION_NOT_SPECIFIED = "Version not specified";

    @Override
    public String[] getDisplayValue(Object nodeObject, int i) {
        TableSyntaxNode tsn = (TableSyntaxNode) nodeObject;
        ITableProperties tableProperties = tsn.getTableProperties();
        if (tableProperties != null) {
            String version = tableProperties.getVersion();
            if (version != null) {
                return new String[] { version, version, version };
            }
        }
        return new String[] { VERSION_NOT_SPECIFIED, VERSION_NOT_SPECIFIED, VERSION_NOT_SPECIFIED };
    }

    @Override
    public String getName() {
        return TABLE_VERSION;
    }

    @Override
    public Object getProblems(Object nodeObject) {
        TableSyntaxNode tsn = (TableSyntaxNode) nodeObject;
        return tsn.getErrors() != null ? tsn.getErrors() : tsn.getValidationResult();
    }

    @Override
    public String getType(Object nodeObject) {
        TableSyntaxNode tsn = (TableSyntaxNode) nodeObject;
        return IProjectTypes.PT_TABLE + "." + tsn.getType();
    }

    @Override
    public String getUrl(Object nodeObject) {
        TableSyntaxNode tsn = (TableSyntaxNode) nodeObject;
        return tsn.getUri();
    }

    @Override
    public int getWeight(Object nodeObject) {
        return 0;
    }

    @Override
    protected Object makeObject(TableSyntaxNode tableSyntaxNode) {
        return tableSyntaxNode;
    }

}
