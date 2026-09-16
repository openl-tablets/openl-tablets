package org.openl.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import org.openl.rules.lang.xls.XlsNodeTypes;

class HeadersWithSpacesTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/test xls/Test_Headers_With_Spaces.xls";

    public HeadersWithSpacesTest() {
        super(SRC);
    }

    @Test
    void testDT() {
        // find the table by the header with first 3 spaces
        //
        var tsn = findTable("Rules DoubleValue getILFactor(String coverageCD, String vehicleGroup)");
        if (tsn == null) {
            fail("Cannot find Decision table");
        } else {
            // https://jira.exigenservices.com/browse/EPBDS-3104, after fix should be DT
            assertEquals(XlsNodeTypes.XLS_DT, XlsNodeTypes.getEnumByValue(tsn.getType()));
        }
    }
}
