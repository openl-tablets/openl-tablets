package org.openl.rules.table.properties;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;

public class PropertiesDefinedInTableTest extends BaseOpenlBuilderHelper {

    public static final String SRC = "test/rules/properties/PropertiesDefinedInTable.xls";

    public PropertiesDefinedInTableTest() {
        super(SRC);
    }

    @Test
    public void testEmptyPropertyValue() {
        TableSyntaxNode tsn = findTable("Method int test()");
        assertNotNull(tsn);
        assertTrue("There are properties in table", tsn.hasPropertiesDefinedInTable());
        assertEquals("No any values of the properties defined in table",
            0,
            tsn.getTableProperties().getTableProperties().size());
    }

    @Test
    public void testNotEmptyPropertyValue() {
        TableSyntaxNode tsn = findTable("Method int test1()");
        assertNotNull(tsn);
        assertTrue("There are properties in table", tsn.hasPropertiesDefinedInTable());
        assertEquals("One property with value in table", 1, tsn.getTableProperties().getTableProperties().size());
    }
}
