package org.openl.rules.datatype.binding;

import static junit.framework.Assert.assertEquals;
import static junit.framework.TestCase.assertNull;

import org.junit.Test;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.XlsHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;

/**
 * Created by dl on 6/17/14.
 */
public class DatatypeSorterTest {

    @Test
    public void testOrderDatatypes_Inheritance_Null() {
        assertNull(DatatypesSorter.sort(null, null));
    }

    @Test
    public void testOrderDatatypes_InheritanceDependency() {
        String[][] parent = new String[1][1];
        parent[0][0] = "Datatype TypeParent";

        String[][] child = new String[1][1];
        child[0][0] = "Datatype TypeChild extends TypeParent";

        String[][] independent = new String[1][1];
        independent[0][0] = "Datatype Independent";

        TableSyntaxNode[] ordered = DatatypesSorter
            .sort(
                new TableSyntaxNode[] { getTableSyntaxNode(child),
                        getTableSyntaxNode(independent),
                        getTableSyntaxNode(parent) },
                null);

        assertEquals(3, ordered.length);
        assertEquals("Parent should be compiled first",
            "Datatype TypeParent",
            ordered[0].getHeader().getModule().getCode());
        assertEquals("Child position goes after parent",
            "Datatype TypeChild extends TypeParent",
            ordered[1].getHeader().getModule().getCode());
        assertEquals("Independent datatype position is not changed",
            "Datatype Independent",
            ordered[2].getHeader().getModule().getCode());
    }

    @Test
    public void testOrderDatatypes_fieldsDependency() {
        String[][] table1 = new String[3][2];
        table1[0][0] = "Datatype Dependent";
        table1[0][1] = null;
        table1[1][0] = "String";
        table1[1][1] = "name";
        table1[2][0] = "Dependence";
        table1[2][1] = "type2Obj";

        String[][] table2 = new String[3][2];
        table2[0][0] = "Datatype Independent";
        table2[0][1] = null;
        table2[1][0] = "Integer";
        table2[1][1] = "num";
        table2[2][0] = "Boolean";
        table2[2][1] = "flag";

        String[][] table3 = new String[3][2];
        table3[0][0] = "Datatype Dependence";
        table3[0][1] = null;
        table3[1][0] = "Integer";
        table3[1][1] = "num";
        table3[2][0] = "Boolean";
        table3[2][1] = "flag";

        TableSyntaxNode[] ordered = DatatypesSorter
            .sort(
                new TableSyntaxNode[] { getTableSyntaxNode(table1),
                        getTableSyntaxNode(table2),
                        getTableSyntaxNode(table3) },
                null);
        assertEquals(3, ordered.length);
        assertEquals("Datatype Dependence", ordered[0].getHeader().getModule().getCode());
        assertEquals("Datatype Dependent", ordered[1].getHeader().getModule().getCode());
        assertEquals("Datatype Independent", ordered[2].getHeader().getModule().getCode());
    }

    @Test
    public void testOrderDatatypes_fieldsDependency_Recursion() {
        String[][] table1 = new String[4][2];
        table1[0][0] = "Datatype Dependent";
        table1[0][1] = null;
        table1[1][0] = "String";
        table1[1][1] = "name";
        table1[2][0] = "Dependence";
        table1[2][1] = "type2Obj";

        // Added Recursion dependency
        //
        table1[3][0] = "Dependent";
        table1[3][1] = "type1Obj";

        String[][] table2 = new String[3][2];
        table2[0][0] = "Datatype Independent";
        table2[0][1] = null;
        table2[1][0] = "Integer";
        table2[1][1] = "num";
        table2[2][0] = "Boolean";
        table2[2][1] = "flag";

        String[][] table3 = new String[3][2];
        table3[0][0] = "Datatype Dependence";
        table3[0][1] = null;
        table3[1][0] = "Integer";
        table3[1][1] = "num";
        table3[2][0] = "Boolean";
        table3[2][1] = "flag";

        TableSyntaxNode[] ordered = DatatypesSorter
            .sort(
                new TableSyntaxNode[] { getTableSyntaxNode(table1),
                        getTableSyntaxNode(table2),
                        getTableSyntaxNode(table3) },
                null);
        assertEquals(3, ordered.length);
        assertEquals("Datatype Dependence", ordered[0].getHeader().getModule().getCode());
        assertEquals("Datatype Dependent", ordered[1].getHeader().getModule().getCode());
        assertEquals("Datatype Independent", ordered[2].getHeader().getModule().getCode());
    }

    @Test(timeout = 100000)
    public void testOrderDatatypes_fieldsDependency_RecursionInInheritance() {
        String[][] tableParent = new String[3][2];
        tableParent[0][0] = "Datatype TypeParent";
        tableParent[0][1] = null;
        tableParent[1][0] = "String";
        tableParent[1][1] = "name";
        // Added Recursion dependency
        tableParent[2][0] = "TypeChild";
        tableParent[2][1] = "typeChild";

        String[][] tableChild = new String[3][2];
        tableChild[0][0] = "Datatype TypeChild extends TypeParent";
        tableChild[0][1] = null;
        tableChild[1][0] = "Integer";
        tableChild[1][1] = "num";
        tableChild[2][0] = "Boolean";
        tableChild[2][1] = "flag";

        // Shouldn't throw StackOverflowError
        TableSyntaxNode[] ordered = DatatypesSorter
            .sort(new TableSyntaxNode[] { getTableSyntaxNode(tableParent), getTableSyntaxNode(tableChild) }, null);
        assertEquals(2, ordered.length);
        assertEquals("Datatype TypeChild extends TypeParent", ordered[0].getHeader().getModule().getCode());
        assertEquals("Datatype TypeParent", ordered[1].getHeader().getModule().getCode());
    }

    @Test
    public void testOrderDatatypes_arrayFieldsDependency() {
        String[][] table1 = new String[3][2];
        table1[0][0] = "Datatype Independent";
        table1[0][1] = null;
        table1[1][0] = "String";
        table1[1][1] = "name";
        table1[2][0] = "Boolean";
        table1[2][1] = "boolVal";

        String[][] table2 = new String[3][2];
        table2[0][0] = "Datatype Dependent";
        table2[0][1] = null;
        table2[1][0] = "Integer";
        table2[1][1] = "num";
        table2[2][0] = "Dependence[]";
        table2[2][1] = "type2Array";

        String[][] table3 = new String[3][2];
        table3[0][0] = "Datatype Dependence";
        table3[0][1] = null;
        table3[1][0] = "Integer";
        table3[1][1] = "num";
        table3[2][0] = "Boolean";
        table3[2][1] = "flag";

        TableSyntaxNode[] ordered = DatatypesSorter
            .sort(
                new TableSyntaxNode[] { getTableSyntaxNode(table1),
                        getTableSyntaxNode(table2),
                        getTableSyntaxNode(table3) },
                null);
        assertEquals(3, ordered.length);
        assertEquals("Datatype Independent", ordered[0].getHeader().getModule().getCode());
        assertEquals("Datatype Dependence", ordered[1].getHeader().getModule().getCode());
        assertEquals("Datatype Dependent", ordered[2].getHeader().getModule().getCode());
    }

    private TableSyntaxNode getTableSyntaxNode(Object[][] cells) {
        try {
            IGridTable gridTable = new MockGridTable(cells);
            return XlsHelper.createTableSyntaxNode(gridTable, null);
        } catch (OpenLCompilationException e) {
            throw new RuntimeException(e);
        }
    }
}
