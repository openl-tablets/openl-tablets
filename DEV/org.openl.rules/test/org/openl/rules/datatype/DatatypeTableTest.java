package org.openl.rules.datatype;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import org.openl.rules.BaseOpenlBuilderHelper;

class DatatypeTableTest extends BaseOpenlBuilderHelper {

    private static final String src = "test/rules/datatype/DatatypeTableTest.xls";

    public DatatypeTableTest() {
        super(src);
    }

    @Test
    void testCanAccessDatatype() {
        var openClass = getCompiledOpenClass().getOpenClass();
        assertNotNull(openClass.findType("Driver"), "There is Driver datatype");
    }

    @Test
    void testDatatypeMember() {
        var node = findTable("Datatype Driver");
        if (node != null) {
            assertEquals("Driver", node.getMember().getName());
        } else {
            fail();
        }

    }

}
