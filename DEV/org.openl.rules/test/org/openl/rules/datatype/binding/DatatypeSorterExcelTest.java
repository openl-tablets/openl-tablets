package org.openl.rules.datatype.binding;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.impl.IdentifierNode;

/**
 * @author PUdalau
 * @author DLiauchuk
 *
 */
public class DatatypeSorterExcelTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/DatatypeInheritanceTest.xls";

    public DatatypeSorterExcelTest() {
        super(SRC);
    }

    private String getDatatypeTypeName(TableSyntaxNode tsn) {
        ILogicalTable table = tsn.getTable();
        IOpenSourceCodeModule src = new GridCellSourceCodeModule(table.getSource());

        try {
            IdentifierNode[] parsedHeader = DatatypeHelper.tokenizeHeader(src);
            return parsedHeader[DatatypeNodeBinder.TYPE_INDEX].getIdentifier();
        } catch (OpenLCompilationException e) {
        }
        return null;
    }

    @Test
    public void test_InheritanceOrder() {
        TableSyntaxNode[] ordered = DatatypesSorter.sort(getTableSyntaxNodes(), null);
        assertEquals(5, ordered.length);
        assertEquals("ParentType", getDatatypeTypeName(ordered[0]));
        assertEquals("ChildType", getDatatypeTypeName(ordered[1]));
        assertEquals("SecondLevelChildType", getDatatypeTypeName(ordered[2]));
        assertEquals("WarnChild", getDatatypeTypeName(ordered[3]));
        assertEquals("ErrorChild", getDatatypeTypeName(ordered[4]));
    }
}
