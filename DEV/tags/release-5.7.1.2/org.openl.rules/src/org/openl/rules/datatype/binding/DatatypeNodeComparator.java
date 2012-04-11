package org.openl.rules.datatype.binding;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;

/**
 * Compares datatype TableSyntaxNodes. This comparing is needed to build queue
 * of datatypes for binding phase(We have to bind parent datatype before all
 * datatypes that inherits it)
 * 
 * @author PUdalau
 * 
 */
public class DatatypeNodeComparator implements Comparator<TableSyntaxNode> {
    private Map<String, TableSyntaxNode> datatypesByName;

    public DatatypeNodeComparator(TableSyntaxNode[] tsns) {
        datatypesByName = new HashMap<String, TableSyntaxNode>();
        for (TableSyntaxNode tsn : tsns) {
            String datatypeName = getDatatypeName(tsn);
            if (datatypeName != null) {
                datatypesByName.put(datatypeName, tsn);
            }
        }
    }

    public int compare(TableSyntaxNode first, TableSyntaxNode second) {
        return getInheritanceLevel(first) - getInheritanceLevel(second);
    }

    private int getInheritanceLevel(TableSyntaxNode tsn) {
        String parent = getParentDatatypeName(tsn);
        if (parent != null && datatypesByName.containsKey(parent)) {
            return 1 + getInheritanceLevel(datatypesByName.get(parent));
        } else {
            return 0;
        }
    }

    private String getDatatypeName(TableSyntaxNode tsn) {
        if (ITableNodeTypes.XLS_DATATYPE.equals(tsn.getType())) {
            ILogicalTable table = LogicalTableHelper.logicalTable(tsn.getTable());
            IOpenSourceCodeModule src = new GridCellSourceCodeModule(table.getGridTable());
            try {
                IdentifierNode[] parsedHeader = Tokenizer.tokenize(src, " \n\r");
                return parsedHeader[DatatypeNodeBinder.TYPE_INDEX].getIdentifier();
            } catch (OpenLCompilationException e) {
            }
        }
        return null;
    }

    private String getParentDatatypeName(TableSyntaxNode tsn) {
        ILogicalTable table = LogicalTableHelper.logicalTable(tsn.getTable());
        IOpenSourceCodeModule src = new GridCellSourceCodeModule(table.getGridTable());
        try {
            IdentifierNode[] parsedHeader = Tokenizer.tokenize(src, " \n\r");
            if (parsedHeader.length == 4) {
                return parsedHeader[DatatypeNodeBinder.PARENT_TYPE_INDEX].getIdentifier();
            } else {
                return null;
            }
        } catch (OpenLCompilationException e) {
            return null;
        }
    }
}
