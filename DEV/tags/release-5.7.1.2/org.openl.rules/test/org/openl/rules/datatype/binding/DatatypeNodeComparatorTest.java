package org.openl.rules.datatype.binding;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.ITableNodeTypes;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.LogicalTableHelper;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;

/**
 * @author PUdalau
 */
public class DatatypeNodeComparatorTest extends BaseOpenlBuilderHelper {
    private static String __src = "test/rules/DatatypeInheritanceTest.xls";

    public DatatypeNodeComparatorTest() {
        super(__src);
    }

    private TableSyntaxNode findTableSyntaxNodeByDatatypeName(String datatypeName) {
        for (TableSyntaxNode tsn : getTableSyntaxNodes()) {
            if (ITableNodeTypes.XLS_DATATYPE.equals(tsn.getType())) {
                ILogicalTable table = LogicalTableHelper.logicalTable(tsn.getTable());
                IOpenSourceCodeModule src = new GridCellSourceCodeModule(table.getGridTable());
                try {
                    IdentifierNode[] parsedHeader = Tokenizer.tokenize(src, " \n\r");
                    if (parsedHeader[DatatypeNodeBinder.TYPE_INDEX].getIdentifier().equals(datatypeName)) {
                        return tsn;
                    }
                } catch (OpenLCompilationException e) {
                }
            }
        }
        return null;
    }

    @Test
    public void testComparing() {
        DatatypeNodeComparator comparator = new DatatypeNodeComparator(getTableSyntaxNodes());
        TableSyntaxNode parent = findTableSyntaxNodeByDatatypeName("ParentType");
        TableSyntaxNode child = findTableSyntaxNodeByDatatypeName("ChildType");
        TableSyntaxNode secondLevelChild = findTableSyntaxNodeByDatatypeName("SecondLevelChildType");
        TableSyntaxNode warnChild = findTableSyntaxNodeByDatatypeName("WarnChild");
        TableSyntaxNode errorChild = findTableSyntaxNodeByDatatypeName("ErrorChild");
        
        assertTrue(comparator.compare(parent, secondLevelChild) < 0);
        assertTrue(comparator.compare(child, errorChild) == 0);
        assertTrue(comparator.compare(parent, warnChild) < 0);
        assertTrue(comparator.compare(child, secondLevelChild) < 0);
        assertTrue(comparator.compare(secondLevelChild, errorChild) > 0);
    }

}
