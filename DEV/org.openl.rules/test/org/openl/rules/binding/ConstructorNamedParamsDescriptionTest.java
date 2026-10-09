package org.openl.rules.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.openl.binding.impl.ConstructorUsage;
import org.openl.rules.BaseOpenlBuilderHelper;

/**
 * A constructor call with named arguments is described with every argument, also with one whose value cannot be
 * assigned to its field.
 */
class ConstructorNamedParamsDescriptionTest extends BaseOpenlBuilderHelper {

    private static final String SRC = "test/rules/binding/ConstructorNamedParamsDescriptionTest.xlsx";

    ConstructorNamedParamsDescriptionTest() {
        super(SRC);
    }

    @Test
    void describesTheArgumentsOfTheCall() {
        // = new MyType(intField = 5, strField = "a")
        assertEquals("Datatype MyType\nMyType (Integer intField, String strField)", descriptionOf(1, 2));
    }

    @Test
    void describesAnArgumentWhoseValueCannotBeAssigned() {
        // = new MyType(intField = "a", strField = "b")
        assertEquals("Datatype MyType\nMyType (Integer intField, String strField)", descriptionOf(1, 3));
    }

    private String descriptionOf(int column, int row) {
        var table = findTable("Spreadsheet SpreadsheetResult make()");
        var cell = table.getGridTable().getCell(column, row);
        var metaInfo = table.getMetaInfoReader().getMetaInfo(cell.getAbsoluteRow(), cell.getAbsoluteColumn());
        assertNotNull(metaInfo);
        var usage = metaInfo.getUsedNodes()
                .stream()
                .filter(ConstructorUsage.class::isInstance)
                .findFirst()
                .orElseThrow();
        return usage.getDescription();
    }
}
