package org.openl.rules.table.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
        assertTrue(tsn.hasPropertiesDefinedInTable(), "There are properties in table");
        assertEquals(0,
                tsn.getTableProperties().getTableProperties().size(),
                "No any values of the properties defined in table");
    }

    @Test
    public void testNotEmptyPropertyValue() {
        TableSyntaxNode tsn = findTable("Method int test1()");
        assertNotNull(tsn);
        assertTrue(tsn.hasPropertiesDefinedInTable(), "There are properties in table");
        assertEquals(1, tsn.getTableProperties().getTableProperties().size(), "One property with value in table");
    }
}
